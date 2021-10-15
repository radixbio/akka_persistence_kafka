package com.radix.shared.persistence.serializations.device_drivers.common

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, NotSerializableException}

import scala.reflect.ClassTag
import akka.actor.ExtendedActorSystem
import akka.serialization.{SerializationExtension, SerializerWithStringManifest}
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.device_drivers.common.requests.Local
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema, Encoder, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.file.{DataFileReader, DataFileStream, DataFileWriter, SeekableByteArrayInput}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.io.{DatumReader, DatumWriter}

object DriverSerializer {

  // does not work for any T that is either not serializer by avro or is not in akka serializers
  //TODO: @dhash please shapeless this with HKT LabelledGenerics so this does not have pasta for every HKT
  class LocalAvro(implicit as: ExtendedActorSystem) extends SerializerWithStringManifest {
    lazy val ser = SerializationExtension(as)

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {

      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(bytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val R = dfs.next.asInstanceOf[GenericRecord]
      val reqSchema = R.getSchema.getField("response").schema()
      val reqManifest = reqSchema.getNamespace + "$" + reqSchema.getName
      val reqRec = R.get("response") // avro object thingie
      val ident = R.get("ident").asInstanceOf[Int]
      val foo = GenericData.get.createDatumWriter(reqSchema).asInstanceOf[DatumWriter[Any]]
      val w = new DataFileWriter(foo)
      val baos = new ByteArrayOutputStream()
      w.create(reqSchema, baos)
      w.append(reqRec)
      w.flush()
      w.close()
      val reqBytes = baos.toByteArray
      println(s"manifest: $reqManifest")
      val req = ser.deserialize(reqBytes, ident, reqManifest)
      Local(req)
    }

    override def toBinary(o: AnyRef): Array[Byte] = {
      val oo = o.asInstanceOf[Local[AnyRef]]
      val reqSer = ser.findSerializerFor(oo.request)
      val reqbytes = reqSer.toBinary(oo.request)
      val dfs = new DataFileStream[Any](
        new ByteArrayInputStream(reqbytes),
        GenericData.get.createDatumReader(null).asInstanceOf[DatumReader[Any]]
      )
      val reqSchema = dfs.getSchema
      val localSchema = SchemaBuilder
        .record("Local")
        .namespace("com.radix.shared.persistence.serializations.device_drivers.common.requests")
        .fields()
        .name("response")
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
      R.put("response", reqObj)
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

    override def identifier: Int = 1023

  }

}