package com.radix.shared.persistence.serializations.squants

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import squants.space.{Length, Volume}
import squants.thermal.Temperature

object schemas {

  implicit object SchemaForTemp extends SchemaFor[Temperature] {
    override def schema(fieldMapper: FieldMapper): Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("temperature")
        .fields()
        .requiredDouble("temperature")
        .requiredString("unit")
        .endRecord()
  }

  implicit object SchemaForLength extends SchemaFor[Length] {
    override def schema(fieldMapper: FieldMapper): Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("length")
        .fields()
        .requiredDouble("length")
        .requiredString("unit")
        .endRecord()
  }

  implicit object SchemaForVolume extends SchemaFor[Volume] {
    override def schema(fieldMapper: FieldMapper): Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("volume")
        .fields()
        .requiredDouble("volume")
        .requiredString("unit")
        .endRecord()
  }

  implicit object TemperatureEncoder extends Encoder[Temperature] {
    override def encode(t: Temperature, schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("temperature", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
  }

  implicit object TemperatureDecoder extends Decoder[Temperature] {
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Temperature = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("temperature").toString + " " + record.get("unit").toString
      Temperature(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Temperature"))
    }
  }

  implicit object LengthEncoder extends Encoder[Length] {
    override def encode(t: Length, schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("length", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
  }

  implicit object LengthDecoder extends Decoder[Length] {
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Length = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("length").toString + " " + record.get("unit").toString
      Length(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Length"))
    }
  }

  implicit object VolumeEncoder extends Encoder[Volume] {
    override def encode(t: Volume, schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("volume", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
  }

  implicit object VolumeDecoder extends Decoder[Volume] {
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Volume = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("volume").toString + " " + record.get("unit").toString
      Volume(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Volume"))
    }
  }

}