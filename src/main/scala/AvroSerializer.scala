package com.radix.shared.persistence

import java.io.{ByteArrayOutputStream, NotSerializableException}

import akka.serialization.SerializerWithStringManifest
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema, Decoder, Encoder, RecordFormat, SchemaFor}

import scala.reflect.ClassTag

class AvroSerializer[T: SchemaFor: ClassTag: Decoder: Encoder]()(implicit ev: T <:< AnyRef)
    extends SerializerWithStringManifest {
  val Manifest = implicitly[ClassTag[T]].toString
  val schema: SchemaFor[T] = implicitly
  val classTag = implicitly[ClassTag[T]]

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
      case classTag(obj) =>
        val os = AvroOutputStream.data[T].to(baos).build()
        os.write(obj)
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

  /**
   * Use Avro to serialize object `o` to an array of bytes.
   * As of now, to/fromBinary use data, which embeds entire schema in each message.
   * First, this is not supported by frontend so we need to use binary which just has data.
   * Second, this is way faster which is good.
   * For examples, look at [[com.radix.frontdoor.gen]]
   */
  def toBinaryFrontend(o: AnyRef): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    o match {
      case classTag(obj) =>
        val os = AvroOutputStream.binary[T].to(baos).build()
        os.write(obj)
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

  /**
   * Use Avro to deserialize an array of bytes into an object with `manifest`.
   * Manifest for us is a class path.
   * As of now, to/fromBinary use data, which embeds entire schema in each message.
   * First, this is not supported by frontend so we need to use binary which just has data.
   * Second, this is way faster which is good.
   * For examples, look at [[com.radix.frontdoor.gen]]
   *
   * @param bytes byte array to deserialize
   * @param manifest manifest of the target to deserialize
   * @return deserialized object
   */
  def fromBinaryFrontend(bytes: Array[Byte], manifest: String): AnyRef = manifest match {

    case Manifest => {
      val is = AvroInputStream.binary[T].from(bytes).build(AvroSchema[T])
      val res: T = is.iterator.next()
      is.close()
      ev.apply(res)
    }

    case els =>
      throw new NotSerializableException(
        s"this serializer only can deserialize for $Manifest, you gave $els, of type ${els.getClass.getName}, with manifest $manifest"
      )
  }
}
