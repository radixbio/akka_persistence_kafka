package com.radix.shared.persistence

import java.io.ByteArrayOutputStream

import akka.serialization.Serialization
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.util.Utf8

object AnySerializedObjectToAvro {
  def apply(implicit ser: Serialization, serializerId: Int, manifest: String, obj: Object): Option[Any] = {
    obj match {
      case null => None
      case gr: GenericRecord =>
        val os = new ByteArrayOutputStream()
        val dw = new GenericDatumWriter[GenericRecord](gr.getSchema)
        val df = new DataFileWriter[GenericRecord](dw)
        df.create(gr.getSchema, os)
        df.append(gr) // tfw you forget to actually add the datum :(
        df.flush()
        df.close()
        val barr = os.toByteArray
        Some(ser.deserialize(barr, serializerId, manifest).get)
      case str: Utf8 => Some(str.toString)
      case els       => Some(els)
    }
  }
}
