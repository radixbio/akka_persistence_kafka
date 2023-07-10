package com.radix.shared.persistence

import java.io.{ByteArrayOutputStream, NotSerializableException}

import akka.serialization.SerializerWithStringManifest
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema, Decoder, Encoder, RecordFormat, SchemaFor}

import scala.reflect.ClassTag

/**
 * @note Do not rename this class, as its name is hard-referenced by AutoSerz
 */
class AvroSerializer[T: SchemaFor: ClassTag: Decoder: Encoder]()(implicit ev: T <:< AnyRef)
    extends SerializerWithStringManifest {
  val Manifest = implicitly[ClassTag[T]].toString
  val schema: SchemaFor[T] = implicitly
  val classTag = implicitly[ClassTag[T]]
  val encoder: Encoder[T] = implicitly
  val decoder: Decoder[T] = implicitly

  /**
   * Deserialize the array of bytes into an object of type [[T]]. Uses Avro binary format to deserialize.
   * Used by Akka serialization.
   * @param bytes bytes to deserialize
   * @param manifest manifest
   * @return deserialized object
   */
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    fromBinary(bytes, manifest, useJson = false)

  /**
   * Deserialize the array of bytes into an object of type [[T]].
   * @param bytes bytes to deserialize
   * @param manifest manifest for the array of bytes to verify the validity of this operation
   * @param useJson true - treat bytes as a UTF-8 encode JSON string
   *                false - treat bytes as an Avro encoded binary string
   * @return deserialized object
   */
  def fromBinary(bytes: Array[Byte], manifest: String, useJson: Boolean): AnyRef = manifest match {
    case Manifest => {
      val is =
        if (useJson) AvroInputStream.json[T].from(bytes).build(AvroSchema[T])
        else AvroInputStream.binary[T].from(bytes).build(AvroSchema[T])
      val res: T = is.iterator.next()
      is.close()
      ev.apply(res)
    }

    case els =>
      throw new NotSerializableException(
        s"this serializer only can deserialize for $Manifest, you gave $els, of type ${els.getClass.getName}, with manifest $manifest"
      )
  }

  /**
   * Serialize object to array of bytes using avro binary format.
   * Used by Akka serialization.
   * @param o object to serialize
   * @return array of bytes
   */
  override def toBinary(o: AnyRef): Array[Byte] = toBinary(o, useJson = false)

  /**
   * Serialize given object into an array of bytes.
   * @param o object to serialize
   * @param useJson true - serialize to a bytes representing an UTF-8 encoded JSON string,
   *                false - serialize to avro encoded binary string
   * @return array of bytes
   */
  def toBinary(o: AnyRef, useJson: Boolean): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    o match {
      case classTag(obj) =>
        val os =
          if (useJson) AvroOutputStream.json[T].to(baos).build()
          else AvroOutputStream.binary[T].to(baos).build()
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
}
