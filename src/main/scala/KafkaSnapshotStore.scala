package com.radix.shared.persistence

import akka.NotUsed
import akka.actor.ActorRef
import akka.actor.typed.ActorSystem
import akka.cluster.ddata.Replicator.{UpdateFailure, UpdateSuccess}
import akka.kafka.Metadata.{EndOffsets, GetEndOffsets, ListTopics, Topics}
import akka.kafka.{CommitterSettings, ConsumerSettings, KafkaConsumerActor, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.persistence.snapshot.SnapshotStore
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}

import scala.collection.immutable.Seq
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class KafkaSnapshotStore(cfg: Config) extends SnapshotStore {

  override def receivePluginInternal: Receive = {
    case success: UpdateSuccess[_] => log.debug(s"update success for ${success.key}")
    case fail: UpdateFailure[_]    => log.error(s"update failed for ${fail.key}")
    case els                       => super.receivePluginInternal(els)
  }

  private val localConfig = new KafkaConfig(cfg)

  val serializationExtension: Serialization = SerializationExtension(context.system)
  implicit val mat: Materializer = Materializer(context.system)
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val producerSettings: ProducerSettings[String, Object] = {
    val valueSerializer = new KafkaAvroSerializer().asInstanceOf[Serializer[Object]]
    valueSerializer.configure(localConfig.avroConfig, false)
    ProducerSettings(localConfig.producerConfig, new StringSerializer, valueSerializer)
      .withBootstrapServers(localConfig.bootstrapServers)
  }
  private val kafkaProducer = producerSettings.createKafkaProducer()

  private val consumerSettings: ConsumerSettings[String, Object] = {
    val valueDeserializer = new KafkaAvroDeserializer().asInstanceOf[Deserializer[Object]]
    valueDeserializer.configure(localConfig.avroConfig, false)
    ConsumerSettings(localConfig.consumerConfig, new StringDeserializer, valueDeserializer)
      .withGroupId("radix-consumers")
      .withBootstrapServers(localConfig.bootstrapServers)
  }
  private val metadataConsumer: ActorRef = context.system.actorOf(KafkaConsumerActor.props(consumerSettings))

  val snapshotPostfix = "-snapshot"

  private def getEndOffset(partition: TopicPartition): Future[Long] =
    for {
      response <- metadataConsumer ? GetEndOffsets(Set(partition))
      endOffsets = response match {
        case EndOffsets(Success(topicOffsetMap)) => topicOffsetMap.values.head
      }
    } yield endOffsets

  private def listTopics() =
    for {
      response <- metadataConsumer ? ListTopics
      topics = response match {
        case Topics(Success(topicMap)) => topicMap.keys.toSet
      }
    } yield topics

  private def loadSnapshots(
    persistenceId: String,
    criteria: SnapshotSelectionCriteria,
  ): Future[Seq[SelectedSnapshot]] = {
    /* We could infer that a topic with an endOffset of zero is invalid,
     * however that would lead to the topic in question being created, which
     * is not what we want. As a result, we must ensure that the topic exists
     * before querying its endOffset.
     */
    val requestedTopic = persistenceId + snapshotPostfix

    listTopics().flatMap { validTopics =>
      if (validTopics.contains(requestedTopic)) {
        val partition = new TopicPartition(requestedTopic, 0)
        val subscription = Subscriptions.assignmentWithOffset(partition, 0)

        for {
          endOffset <- getEndOffset(partition)

          records <- Consumer
            .plainSource(consumerSettings, subscription)
            .map(record => (KafkaSnapshotKey(record.key), record))
            .dropWhile { case (key, _) => key.sequenceNr < criteria.minSequenceNr }
            .takeWhile({ case (_, record) => record.offset < endOffset - 1 }, inclusive = true)
            .filter {
              case (key, _) =>
                criteria.minSequenceNr <= key.sequenceNr && key.sequenceNr <= criteria.maxSequenceNr
            }
            .filter {
              case (key, _) =>
                criteria.minTimestamp <= key.timestamp && key.timestamp <= criteria.maxTimestamp
            }
            .runWith(Sink.seq)

        } yield {
          records
            /* In the event two records have the same sequence Id,
             * prefer the one with the largest offset.
             */
            .groupBy { case (key, _) => key.sequenceNr }
            .mapValues(_.maxBy { case (_, record) => record.offset })
            .values
            .toList
            /* At this point we have converged on a single record per sequence ID,
             * in ascending order. However, some records may have been deleted, and
             * we must filter them out at this time.
             */
            .filter { case (_, record) => record.value != null }
            .sortBy { case (key, _) => key.sequenceNr }
            .sortBy { case (key, _) => key.sequenceNr }
            .map {
              case (key, record) =>
                val metadata = SnapshotMetadata(persistenceId, key.sequenceNr, key.timestamp)
                SelectedSnapshot(metadata, record.value)
            }
        }
      } else {
        Future.successful(Seq.empty)
      }
    }
  }

  override def loadAsync(
    persistenceId: String,
    criteria: SnapshotSelectionCriteria,
  ): Future[Option[SelectedSnapshot]] = {
    for {
      matchingSnapshots <- loadSnapshots(persistenceId, criteria)
      /* The records are in ascending order, so we opt for the last item in
       * the list.
       */
      result = matchingSnapshots.lastOption
    } yield result
  }

  override def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    Source
      .single(AnyAvroToSerializedObject(serializationExtension, snapshot))
      .mapAsync(1)(Future.fromTry)
      .map {
        case (_, obj) =>
          new ProducerRecord(metadata.persistenceId + snapshotPostfix, 0, KafkaSnapshotKey(metadata).toString, obj)
      }
      .map { pr =>
        ProducerMessage.single(pr, NotUsed)
      }
      .via(Producer.flexiFlow(producerSettings, kafkaProducer))
      .runWith(Sink.ignore)
      .map(_ => ())
  }

  override def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    /* Even though SnapshotMetadata contains a timestamp field, we can not
     * rely on it, since a snapshot is uniquely identified by a sequence ID.
     * The TCK tests for this by trying to delete a snapshot with a valid
     * sequence ID but a timestamp of zero.
     *
     * Therefore, we make the time window as large as possible in our search
     * for the most current record. Our goal is to locate the timestamp
     * associated with the record, which we can't assume is contained in the
     * metadata passed to this function.
     */
    val requestedSnapshotCriteria = SnapshotSelectionCriteria(metadata.sequenceNr, Long.MaxValue, metadata.sequenceNr)
    val requestedSnapshot = loadAsync(metadata.persistenceId, requestedSnapshotCriteria)

    requestedSnapshot
      .flatMap {
        case None => Future.successful() // Already deleted, so stop here.
        case Some(snapshot) =>
          val seqNrToDelete = snapshot.metadata.sequenceNr
          val timestampToDelete = snapshot.metadata.timestamp
          val deletionCriteria =
            SnapshotSelectionCriteria(seqNrToDelete, timestampToDelete, seqNrToDelete, timestampToDelete)
          deleteAsync(metadata.persistenceId, deletionCriteria)
      }
  }

  override def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    val requestedTopic = persistenceId + snapshotPostfix

    for {
      matchingSnapshots <- loadSnapshots(persistenceId, criteria)
      deletionRecords = matchingSnapshots.map { snapshot =>
        Console.println(s"emitting: $snapshot")
        new ProducerRecord(requestedTopic, 0, KafkaSnapshotKey(snapshot.metadata).toString, null.asInstanceOf[Object])
      }

      producerMessage = ProducerMessage.multi(deletionRecords, NotUsed)

      flow <- Source
        .single(producerMessage)
        .via(Producer.flexiFlow(producerSettings, kafkaProducer))
        .runWith(Sink.ignore)
        .map(_ => ())
    } yield flow
  }
}
