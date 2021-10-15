package com.radix.shared.persistence.serializations.device_drivers.archetypes

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID
import java.util.function.Supplier

import scala.concurrent.duration.FiniteDuration
import scala.language.existentials
import scala.reflect.ClassTag
import akka.actor.typed.ActorRef
import akka.actor.ExtendedActorSystem
import akka.serialization.{Serialization, SerializationExtension, Serializer, SerializerWithStringManifest}
import akka.stream.SourceRef
import com.radix.rainbow.URainbow.RainbowModifyCommand
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.sksamuel.avro4s.{AvroSchema, Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.file.{DataFileStream, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.io.{DatumReader, DatumWriter}
import akka.actor.typed.scaladsl.adapter._
import org.apache.avro.util.Utf8

import scala.util.{Failure, Success}

object transactions {

  sealed trait Caps
  sealed trait LHCaps extends Caps
  case object NeedsTips extends LHCaps

  sealed trait BioreactorCaps extends Caps
  case class RPMMinMax(min: Int, max: Int) extends BioreactorCaps

  case class CapabilitySet(s: Set[Caps])

  sealed trait TxnMsg

  sealed trait CapTxn extends TxnMsg
  case class QueryCaps(rto: ActorRef[QueryCapsStatus]) extends CapTxn

  sealed trait Txn extends TxnMsg
  case class Command[A](cmd: A, rto: ActorRef[CommandStatus]) extends Txn
  case class Begin(rto: ActorRef[BeginStatus]) extends Txn
  case class Commit(rto: ActorRef[CommitStatus]) extends Txn
  case class Cancel(rto: ActorRef[CancelStatus]) extends Txn
  case class Simulate(rto: ActorRef[SimulationStatus]) extends Txn
  case class QueryUUID(rto: ActorRef[QueryUUIDStatus]) extends Txn


  sealed trait CommandStatus
  case class CommandSuccessful() extends CommandStatus
  case class CommandFailed(cause: String) extends CommandStatus

  sealed trait BeginStatus
  case class BeginSuccessful() extends BeginStatus
  case class BeginFailed(cause: String) extends BeginStatus

  sealed trait CommitStatus
  sealed trait CommitSuccessful extends CommitStatus
  case class CommitWithRainbow(updates: SourceRef[RainbowModifyCommand]) extends CommitSuccessful
  case class CommitNoUpdate() extends CommitSuccessful
  case class CommitFailed(cause: String) extends CommitStatus
  case class DeviceBusyRejected() extends CommitStatus
  case class DeviceBusyEnqueued() extends CommitStatus

  sealed trait CancelStatus
  case class CancelSuccessful() extends CancelStatus
  case class CancelFailed(cause: String) extends CancelStatus

  sealed trait SimulationStatus
  case class SimulationSuccessful(time: squants.time.Time) extends SimulationStatus
  case class SimulationFailed(cause: String) extends SimulationStatus

  sealed trait QueryUUIDStatus
  case class QueryUUIDSuccessful(uuid: UUID) extends QueryUUIDStatus
  case class QueryUUIDFailed(cause: String) extends QueryUUIDStatus

  sealed trait QueryCapsStatus
  case class QueryCapsSuccessful(s: CapabilitySet) extends QueryCapsStatus
  case class QueryCapsFailed(cause: String) extends QueryCapsStatus

  case class DelayedGet[F[_], T](private val underlying: F[T]) {
    def get: F[T] = underlying
  }

  //TODO: @dhash please add shapeless for HKT's here
  class CommandAvro(implicit as: ExtendedActorSystem) extends SerializerWithStringManifest {
    lazy val ser = SerializationExtension(as)

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(bytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val R = dfs.next.asInstanceOf[GenericRecord]
      val reqSchema = R.getSchema.getField("cmd").schema()
      val reqManifest = reqSchema.getNamespace + "$" + reqSchema.getName
      val reqRec = R.get("cmd") // avro object thingie
      val ident = R.get("ident").asInstanceOf[Int]
      val rtoStr = R.get("rto").asInstanceOf[Utf8].toString
      val rto = as.provider.resolveActorRef(rtoStr)
      val foo = GenericData.get.createDatumWriter(reqSchema).asInstanceOf[DatumWriter[Any]]
      val w = new DataFileWriter(foo)
      val baos = new ByteArrayOutputStream()
      w.create(reqSchema, baos)
      w.append(reqRec)
      w.flush()
      w.close()
      val reqBytes = baos.toByteArray
      val req = ser.deserialize(reqBytes, ident, reqManifest) match {
        case Failure(exception) =>
          throw new IllegalStateException(s"got $exception when trying to deserialize $manifest")
        case Success(value) => value
      }
      Command(req, rto)
    }

    override def toBinary(o: AnyRef): Array[Byte] = {
      val oo = o.asInstanceOf[Command[AnyRef]]
      val reqSer = ser.findSerializerFor(oo.cmd)
      val reqbytes = reqSer.toBinary(oo.cmd)
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val reqSchema = dfs.getSchema
      val localSchema = SchemaBuilder
        .record("Command")
        .namespace("com.radix.shared.persistence.serializations.device_drivers.archetypes")
        .fields()
        .name("cmd")
        .`type`(reqSchema)
        .noDefault()
        .name("ident")
        .`type`(AvroSchema[Int])
        .noDefault()
        .name("rto")
        .`type`(AvroSchema[String])
        .noDefault()
        .endRecord()
      val R = new GenericData.Record(localSchema)
      val Tdfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(reqSchema).asInstanceOf[DatumReader[Any]]
      )
      val reqObj = Tdfs.next().asInstanceOf[GenericRecord]
      R.put("cmd", reqObj)
      R.put("ident", reqSer.identifier)
      R.put("rto", Serialization.serializedActorPath(oo.rto.toClassic))
      val foo = GenericData.get.createDatumWriter(R.getSchema).asInstanceOf[DatumWriter[Any]]
      val w = new DataFileWriter(foo)
      val baos = new ByteArrayOutputStream()
      w.create(R.getSchema, baos)
      w.append(R)
      w.flush()
      w.close()
      baos.toByteArray
    }

    override def manifest(o: AnyRef): String = o.getClass.getName

    override def identifier: Int = 1923
  }

  class BeginAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[Begin]
  class CommitAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[Commit]
  class CancelAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[Cancel]
  class SimulateAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[Simulate]
  class QueryUUIDAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryUUID]
  class QueryCapsAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryCaps]
  class CommandSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandSuccessful]
  class CommandFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandFailed]
  class BeginSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[BeginSuccessful]
  class BeginFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[BeginFailed]
  class CommitWithRainbowAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommitWithRainbow]
  class CommitNoUpdateAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommitNoUpdate]
  class CommitFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommitFailed]
  class DeviceBusyEnqueuedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[DeviceBusyEnqueued]
  class DeviceBusyRejectedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[DeviceBusyRejected]
  class CancelSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CancelSuccessful]
  class CancelFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CancelFailed]
  class SimulationSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SimulationSuccessful]
  class SimulationFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SimulationFailed]
  class QueryUUIDSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryUUIDSuccessful]
  class QueryUUIDFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryUUIDFailed]
  class QueryCapsSuccessfulAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryCapsSuccessful]
  class QueryCapsFailedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryCapsFailed]
  


}
