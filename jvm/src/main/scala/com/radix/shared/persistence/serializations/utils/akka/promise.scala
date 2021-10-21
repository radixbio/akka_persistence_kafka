package com.radix.shared.persistence.serializations.utils.akka

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.serialization.{Serialization, SerializationExtension, SerializerWithStringManifest}
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.SchemaBuilder
import org.apache.avro.file.{DataFileStream, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.io.{DatumReader, DatumWriter}
import org.apache.avro.util.Utf8

import scala.util.{Failure, Success}

/**
 * This stuff exists to provide promise functionality across the cluster. Promise in a local sense is just a box, and
 * you can either wait for something to appear inside the box, or put something in a box. Akka does not provide such
 * functionality out of the box. Works as follows: you create a promise actor, and send a Query msg to it. Then, when
 * this promise actor receives a fulfill msg, it will send that value to every actor who
 */
object promise {
  sealed trait PromiseReq
  case class Fulfill[A](value: A) extends PromiseReq
  case class Cancel() extends PromiseReq
  case class Query(replyTo: ActorRef[PromiseResp]) extends PromiseReq

  sealed trait PromiseResp
  case class PromiseFulfilled[A](value: A) extends PromiseResp
  case class PromiseFailed(cause: String) extends PromiseResp

  type PromiseRef = ActorRef[PromiseReq]

  class CancelAvro extends AvroSerializer[Cancel]
  class QueryAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[Query]

  class PromiseFailedAvro extends AvroSerializer[PromiseFailed]

  //TODO: @dhash please add shapeless for HKT's here
  class FulfillAvro(implicit as: ExtendedActorSystem) extends SerializerWithStringManifest {
    lazy val ser = SerializationExtension(as)

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(bytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val R = dfs.next.asInstanceOf[GenericRecord]
      val reqSchema = R.getSchema.getField("value").schema()
      val reqManifest = reqSchema.getNamespace + "$" + reqSchema.getName
      val reqRec = R.get("value") // avro object thingie
      val ident = R.get("ident").asInstanceOf[Int]
      val foo = GenericData.get.createDatumWriter(reqSchema).asInstanceOf[DatumWriter[Any]]
      val w = new DataFileWriter(foo)
      val baos = new ByteArrayOutputStream()
      w.create(reqSchema, baos)
      w.append(reqRec)
      w.flush()
      w.close()
      val reqBytes = baos.toByteArray
      val req = ser.deserialize(reqBytes, ident, reqManifest) match {
        case Failure(exception) => {
          val reqManifestTry2 = reqSchema.getNamespace + "." + reqSchema.getName
          ser.deserialize(reqBytes, ident, reqManifestTry2) match {
            case Failure(exception) => throw new IllegalStateException(s"got $exception when trying to deserialize $manifest")
            case Success(value) => value
          }
        }
        case Success(value) => value
      }
      Fulfill(req)
    }

    override def toBinary(o: AnyRef): Array[Byte] = {
      val oo = o.asInstanceOf[Fulfill[AnyRef]]
      val reqSer = ser.findSerializerFor(oo.value)
      val reqbytes = reqSer.toBinary(oo.value)
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val reqSchema = dfs.getSchema
      val localSchema = SchemaBuilder
        .record("Fulfill")
        .namespace("com.radix.shared.persistence.serializations.utils.akka")
        .fields()
        .name("value")
        .`type`(reqSchema)
        .noDefault()
        .name("ident")
        .`type`(AvroSchema[Int])
        .noDefault()
        .endRecord()
      val R = new GenericData.Record(localSchema)
      val Tdfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(reqSchema).asInstanceOf[DatumReader[Any]]
      )
      val reqObj = Tdfs.next().asInstanceOf[GenericRecord]
      R.put("value", reqObj)
      R.put("ident", reqSer.identifier)
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

    override def identifier: Int = 192348
  }

  //TODO: @dhash please add shapeless for HKT's here
  class PromiseFulfilledAvro(implicit as: ExtendedActorSystem) extends SerializerWithStringManifest {
    lazy val ser = SerializationExtension(as)

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(bytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val R = dfs.next.asInstanceOf[GenericRecord]
      val reqSchema = R.getSchema.getField("cmd").schema()
      val reqManifest = reqSchema.getNamespace + "$" + reqSchema.getName
      val reqRec = R.get("value") // avro object thingie
      val ident = R.get("ident").asInstanceOf[Int]
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
          val reqManifestTry2 = reqSchema.getNamespace + "." + reqSchema.getName
          ser.deserialize(reqBytes, ident, reqManifestTry2) match {
            case Failure(exception) => throw new IllegalStateException(s"got $exception when trying to deserialize $manifest")
            case Success(value) => value
          }
        case Success(value) => value
      }
      PromiseFulfilled(req)
    }

    override def toBinary(o: AnyRef): Array[Byte] = {
      val oo = o.asInstanceOf[PromiseFulfilled[AnyRef]]
      val reqSer = ser.findSerializerFor(oo.value)
      val reqbytes = reqSer.toBinary(oo.value)
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val reqSchema = dfs.getSchema
      val localSchema = SchemaBuilder
        .record("PromiseFulfilled")
        .namespace("com.radix.shared.persistence.serializations.utils.akka")
        .fields()
        .name("value")
        .`type`(reqSchema)
        .noDefault()
        .name("ident")
        .`type`(AvroSchema[Int])
        .noDefault()
        .endRecord()
      val R = new GenericData.Record(localSchema)
      val Tdfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(reqSchema).asInstanceOf[DatumReader[Any]]
      )
      val reqObj = Tdfs.next().asInstanceOf[GenericRecord]
      R.put("value", reqObj)
      R.put("ident", reqSer.identifier)
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

    override def identifier: Int = 192347
  }
}
