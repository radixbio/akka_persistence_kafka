package com.radix.shared.persistence

import akka.serialization.Serialization
import org.apache.avro.file.{DataFileReader, SeekableByteArrayInput}
import org.apache.avro.generic.{GenericDatumReader, GenericRecord, IndexedRecord}
import org.apache.avro.io.DatumReader

import scala.util.{Success, Try}

object AnyAvroToSerializedObject {
  def apply(implicit ser: Serialization, in: Any): Try[(Option[Int], Object)] = {
    in match {
      case null             => Success(None, null)
      case str: String      => Success(None, str)
      case int: Int         => Success(None, int.asInstanceOf[Object])
      case integer: Integer => Success(None, integer.asInstanceOf[Object])
      //case barray: Array[Int] => // serialization as byte array preferred over primitive
      case long: Long        => Success(None, long.asInstanceOf[Object])
      case double: Double    => Success(None, double.asInstanceOf[Object])
      case float: Float      => Success(None, float.asInstanceOf[Object])
      case gr: GenericRecord => Success(None, gr) // allow external serialization
      case ir: IndexedRecord => Success(None, ir)
      case other =>
        Try {
          val otherSerializer = ser.findSerializerFor(other.asInstanceOf[AnyRef])
          val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord]()
          val freader =
            new DataFileReader[GenericRecord](
              new SeekableByteArrayInput(otherSerializer.toBinary(other.asInstanceOf[AnyRef])),
              reader
            )
          val res = freader.iterator().next()
          assert(!freader.hasNext)
          (Some(otherSerializer.identifier), res.asInstanceOf[Object])
          /* Do not try to hide the fact that serialization failed. This should fail noisily.
         * .recover { case exn: Exception => otherSerializer.toBinary(other.asInstanceOf[AnyRef]) }
         */
        }
    }
  }
}
