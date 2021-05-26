package com.radix.shared.persistence.serializations.device_drivers.common

import java.io.{ByteArrayOutputStream, NotSerializableException}
import java.nio.charset.StandardCharsets
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

import akka.actor.ExtendedActorSystem
import akka.serialization.{SerializationExtension, SerializerWithStringManifest}
import com.sksamuel.avro4s.{AvroOutputStream, AvroSchema, Decoder, Encoder, SchemaFor}

import com.radix.shared.persistence.AvroSerializer

object DriverSerializer {

  class Local(system: ExtendedActorSystem) extends SerializerWithStringManifest {
    private lazy val akkaSzr = SerializationExtension(system)
    
    private val Manifest = classOf[requests.Local[_]].getName

    override def identifier: Int = Manifest.hashCode

    override def manifest(o: AnyRef): String = o.getClass.getName

    override def toBinary(o: AnyRef): Array[Byte] = {
      o match {
        case req: requests.Local[_] =>
          akkaSzr.serialize(req.request.asInstanceOf[AnyRef]) match {
            case Failure(exception) =>  throw new NotSerializableException(
              s"Serialization failed as message type could not be serialized: ${exception.getMessage}"
            )
            case Success(value) =>
              encodeString(req.ct.toString()) ++ value
          }
        case els =>
          throw new NotSerializableException(
            s"this serializer can only serialize requests.Local, you gave $els, of type ${els.getClass.getName}"
          )
      }
    }

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
      case Manifest =>
        val (className, consumed) = decodeString(bytes)
        val clazz = Class.forName(className)
        akkaSzr.deserialize(bytes.drop(consumed), clazz) match {
          case Failure(exception) => throw new NotSerializableException(
            s"Serialization failed as message type could not be deserialized: ${exception.getMessage}"
          )
          case Success(msg) =>
            val ct = ClassTag(Class.forName(className)).asInstanceOf[ClassTag[Any]]
            val r = clazz.cast(msg)
            requests.Local(r)(ct).asInstanceOf[AnyRef]
        }

      case other =>
        throw new NotSerializableException(
          s"this serializer only can deserialize for $Manifest, you gave $other, with manifest $manifest"
        )
    }

    private def encodeString(str: String): Array[Byte] = {
      val lbytes = encodeInt(str.length)
      val sbytes = str.getBytes(StandardCharsets.UTF_8)

      lbytes ++ sbytes
    }
    private def decodeString(src: Array[Byte]): (String, Int) = {
      val (len, consumed) = decodeInt(src)

      val str = src.slice(consumed, consumed + len).map(_.toChar).mkString
      (str, len+consumed)
    }

    private final val eiMask = Int.MaxValue - 127
    private def encodeInt(num: Int): Array[Byte] = {
      val zz = if (num >= 0) {
        num * 2
      } else {
        (num * -2) - 1
      }

      doGroup(zz)
    }
    private def doGroup(num: Int): Array[Byte] = {
      if ((num & eiMask) == 0) {
        Array(num.toByte)
      } else {
        Array(((num & 0x7F) | 0x80).toByte) ++ doGroup(num >>> 7)
      }
    }
    private def decodeInt(src: Array[Byte]): (Int, Int) = {
      val unVared = decodeUInt(src)
      if (unVared._1 % 2 == 0) {
        (unVared._1 / 2, unVared._2)
      } else {
        (((unVared._1 / 2) + 1) * -1, unVared._2)
      }
    }
    private def decodeUInt(src: Array[Byte], shift:Int = 0): (Int, Int) = {
      src.headOption match {
        case None => (0, 0)
        case Some(b) =>
          if ((b & 0x80) == 0) {
            ((b & 0x7F) << (shift * 7), shift+1)
          } else {
            val rem = decodeUInt(src.tail, shift+1)
            val x = ((b & 0x7F) << (shift*7)) | rem._1
            (x, rem._2)
          }
      }
    }
  }

}
