package com.radix.shared.persistence.query

import akka.NotUsed
import akka.actor.{ActorContext, ActorRef, ExtendedActorSystem}
import akka.kafka.Metadata.{EndOffsets, GetEndOffsets}
import akka.kafka.{ConsumerSettings, KafkaConsumerActor, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.persistence.query.{scaladsl, EventEnvelope, NoOffset}
import akka.serialization.{Serialization, SerializationExtension}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.radix.shared.persistence.{AnySerializedObjectToAvro, KafkaConfig, KafkaJournal, KafkaJournalKey}
import com.typesafe.config.Config
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Success

class KafkaScalaReadJournal(system: ExtendedActorSystem, cfg: Config)
    extends scaladsl.ReadJournal
    with scaladsl.CurrentPersistenceIdsQuery
    with scaladsl.EventsByPersistenceIdQuery
    with scaladsl.CurrentEventsByPersistenceIdQuery {

  val localConfig: KafkaConfig = new KafkaConfig(cfg)
  implicit val timeout: Timeout = Timeout(5.seconds)
  //implicit val context: ActorContext = system
  //implicit val mat: Materializer = Materializer(system)

  private val consumerSettings: ConsumerSettings[String, Object] = {
    val valueDeserializer = new KafkaAvroDeserializer().asInstanceOf[Deserializer[Object]]
    valueDeserializer.configure(localConfig.avroConfig, false)
    ConsumerSettings(localConfig.consumerConfig, new StringDeserializer, valueDeserializer)
      .withGroupId("radix-consumers")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
      .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000")
      .withBootstrapServers(localConfig.bootstrapServers)
  }
  private val kafkaConsumer = consumerSettings.createKafkaConsumer()
  private val metadataConsumer: ActorRef = system.actorOf(KafkaConsumerActor.props(consumerSettings))

  val serializationExtension: Serialization = SerializationExtension(system)

  /*private def getEndOffset(partition: TopicPartition): Future[Long] = for {
    response <- metadataConsumer ? GetEndOffsets(Set(partition))
    endOffsets = response match {
      case EndOffsets(Success(topicOffsetMap)) => topicOffsetMap.values.head
    }
  } yield endOffsets*/

  override def currentPersistenceIds(): Source[String, NotUsed] =
    Source
      .fromIterator(() => kafkaConsumer.listTopics.keySet.iterator.asScala)
      .filter { topic =>
        topic.endsWith(KafkaJournal.journalPostfix)
      }

  override def eventsByPersistenceId(
    persistenceId: String,
    fromSequenceNr: Long,
    toSequenceNr: Long,
  ): Source[EventEnvelope, NotUsed] = {
    val topic = persistenceId + KafkaJournal.journalPostfix

    val partition = new TopicPartition(topic, 0)
    val subscription = Subscriptions.assignmentWithOffset(partition, 0)

    Consumer
      .plainSource[String, Object](consumerSettings, subscription)
      .map(record => (KafkaJournalKey(record.key), record))
      .filter { case (key, _) => fromSequenceNr <= key.sequenceNr && toSequenceNr >= key.sequenceNr }
      // TODO This groupBy will be extremely memory intensive. Figure out a better way.
      .groupBy(toSequenceNr.toInt, { case (key, _) => key.sequenceNr })
      .reduce { (best, next) =>
        if (next._1.sequenceNr > best._1.sequenceNr) next else best
      }
      // TODO It's not evident that this will preserve the order of the elements.
      .mergeSubstreams
      .map {
        case (key, record) => {
          val deserializedObjectO =
            AnySerializedObjectToAvro(serializationExtension, key.serializerId, key.manifest, record.value)
          EventEnvelope(NoOffset, persistenceId, key.sequenceNr, deserializedObjectO)
        }
      }
      .mapMaterializedValue(_ => NotUsed)
  }

  override def currentEventsByPersistenceId(
    persistenceId: String,
    fromSequenceNr: Long,
    toSequenceNr: Long,
  ): Source[EventEnvelope, NotUsed] = ???
}
