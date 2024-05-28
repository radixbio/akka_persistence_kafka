package com.radix.shared.persistence

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}

import java.nio.file.Path

object PathSerializer {
  implicit def SchemaForPath: SchemaFor[Path] = new SchemaFor[Path] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit object EncoderForPath extends Encoder[Path] {
    override def encode(schema: Schema): Path => AnyRef = (value: Path) => value.toString
  }

  implicit object DecoderForPath extends Decoder[Path] {
    override def decode(schema: Schema): Any => Path = (value: Any) => Path.of(value.toString)
  }
}
