package com.radix.shared.persistence

import java.io.ByteArrayOutputStream
import akka.serialization.Serialization
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.util.Utf8

import scala.util.{Failure, Success, Try}

object AnySerializedObjectToAvro {
  def apply(implicit ser: Serialization, serializerId: Int, manifest: String, obj: Object): Option[Any] = {
    obj match {
      case null      => None
      case str: Utf8 => Some(str.toString)
      case gr: GenericRecord =>
        val avro = ser.serializerOf(manifest).asInstanceOf[AvroSerializer[Any]]
        Try {
          avro.decoder.decode(gr)
        } match {
          case Success(value)     => Some(value)
          case Failure(exception) => None
        }
      case els => Some(els)
    }
  }
}
