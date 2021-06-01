package com.radix.shared.persistence.serializations.squants

import com.radix.shared.persistence.serializations.squants.units._
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{Schema, SchemaBuilder}
import squants.motion.VolumeFlow
import squants.space.{Length, Volume}
import squants.thermal.Temperature
import squants.time.Frequency

object schemas {

  implicit object SchemaForTemp extends SchemaFor[Temperature] {
    override def schema: Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("temperature")
        .fields()
        .requiredDouble("temperature")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForLength extends SchemaFor[Length] {
    override def schema: Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("length")
        .fields()
        .requiredDouble("length")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }

  implicit object SchemaForVolumeFlow extends SchemaFor[VolumeFlow] {
    override def schema: Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("volumeflow")
        .fields()
        .requiredDouble("volumeflow")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForVolume extends SchemaFor[Volume] {
    override def schema: Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("volume")
        .fields()
        .requiredDouble("volume")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }

  implicit object SchemaForFrequency extends SchemaFor[Frequency] {
    override def schema: Schema =
      SchemaBuilder
        .builder("org.typelevel.squants")
        .record("frequency")
        .fields()
        .requiredDouble("frequency")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object TemperatureEncoder extends Encoder[Temperature] {
    override def encode(t: Temperature): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("temperature", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
    override def schemaFor: SchemaFor[Temperature] = SchemaForTemp

  }

  implicit object TemperatureDecoder extends Decoder[Temperature] {
    override def decode(value: Any): Temperature = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("temperature").toString + " " + record.get("unit").toString
      Temperature(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Temperature"
        )
      )
    }
    override def schemaFor: SchemaFor[Temperature] = SchemaForTemp
  }

  implicit object LengthEncoder extends Encoder[Length] {
    override def encode(t: Length): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("length", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
    override def schemaFor: SchemaFor[Length] = SchemaForLength
  }

  implicit object LengthDecoder extends Decoder[Length] {
    override def decode(value: Any): Length = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("length").toString + " " + record.get("unit").toString
      Length(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Length"
        )
      )
    }
    override def schemaFor: SchemaFor[Length] = SchemaForLength
  }

  implicit object VolumeEncoder extends Encoder[Volume] {
    override def encode(t: Volume): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("volume", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
    override def schemaFor: SchemaFor[Volume] = SchemaForVolume
  }

  implicit object VolumeDecoder extends Decoder[Volume] {
    override def decode(value: Any): Volume = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("volume").toString + " " + record.get("unit").toString
      Volume(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Volume"
        )
      )
    }
    override def schemaFor: SchemaFor[Volume] = SchemaForVolume
  }

  implicit object VolumeFlowEncoder extends Encoder[VolumeFlow] {
    def encode(t: VolumeFlow): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("volumeflow", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
    override def schemaFor: SchemaFor[VolumeFlow] = SchemaForVolumeFlow
  }

  implicit object VolumeFlowDecoder extends Decoder[VolumeFlow] {
    override def decode(value: Any): VolumeFlow = {
      val record = value.asInstanceOf[GenericRecord]
      val doubleValue = record.get("volumeflow").toString.toDouble
      val unit = record.get("unit").toString
      val parsed = doubleValue + " " + unit
      VolumeFlow(parsed).getOrElse(
        if (unit == LitresPerMinute.symbol) {
          LitresPerMinute(doubleValue)
        } else {
          throw new IllegalStateException(
            s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.VolumeFlow"
          )
        }
      )
    }
    override def schemaFor: SchemaFor[VolumeFlow] = SchemaForVolumeFlow
  }

  implicit object FrequencyEncoder extends Encoder[Frequency] {
    override def encode(t: Frequency): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("frequency", t.value)
      record.put("unit", t.unit.symbol)
      record
    }

    override def schemaFor: SchemaFor[Frequency] = SchemaForFrequency
  }

  implicit object FrequencyDecoder extends Decoder[Frequency] {
    override def decode(value: Any): Frequency = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("frequency").toString + " " + record.get("unit").toString
      Frequency(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Frequency"
        )
      )
    }

    override def schemaFor: SchemaFor[Frequency] = SchemaForFrequency
  }

}
