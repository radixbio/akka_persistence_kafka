package com.radix.shared.persistence

import java.io.{ByteArrayOutputStream, NotSerializableException}

import akka.serialization.SerializerWithStringManifest
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema, Decoder, Encoder, RecordFormat, SchemaFor}

import scala.reflect.ClassTag

class AvroSerializer[T: SchemaFor: ClassTag: Decoder: Encoder]()(implicit ev: T <:< AnyRef)
    extends SerializerWithStringManifest {
  val Manifest = implicitly[ClassTag[T]].toString
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case Manifest => {
      val is = AvroInputStream.data[T].from(bytes).build(AvroSchema[T])
      val res: T = is.iterator.next()
      is.close()
      ev.apply(res)
    }

    case els =>
      throw new NotSerializableException(
        s"this serializer only can deserialize for $Manifest, you gave $els, of type ${els.getClass.getName}, with manifest $manifest"
      )
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    o match {
      case mf: T =>
        val os = AvroOutputStream.data[T].to(baos).build()
        os.write(mf)
        os.flush()
        os.close()
      case els =>
        throw new NotSerializableException(
          s"this serializer can only serialize $Manifest, you gave $els, of type ${els.getClass.getName}"
        )
    }
    baos.flush()
    val res = baos.toByteArray
    baos.close()
    res
  }

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def identifier: Int = implicitly[ClassTag[T]].toString.hashCode()
}
