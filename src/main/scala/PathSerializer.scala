package com.radix.shared.persistence

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}

import java.nio.file.Path

object PathSerializer {
  implicit object SchemaForPath extends SchemaFor[Path] {
    override def schema: Schema = SchemaBuilder.builder.stringType()

    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object EncoderForPath extends Encoder[Path] {
    override def encode(value: Path): AnyRef = value.toString

    override def schemaFor: SchemaFor[Path] = SchemaForPath
  }

  implicit object DecoderForPath extends Decoder[Path] {
    override def decode(value: Any): Path = Path.of(value.toString)

    override def schemaFor: SchemaFor[Path] = SchemaForPath
  }
}
