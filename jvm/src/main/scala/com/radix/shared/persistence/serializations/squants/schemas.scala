package com.radix.shared.persistence.serializations.squants

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.units._
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{Schema, SchemaBuilder}
import squants.motion.{Velocity, VolumeFlow}
import squants.space.{Length, Volume}
import squants.thermal.Temperature
import squants.time.{Frequency, Time}

object schemas {

  implicit object SchemaForTime extends SchemaFor[Time] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.time")
        .record("Time")
        .fields()
        .requiredDouble("time")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForTemp extends SchemaFor[Temperature] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.thermal")
        .record("Temperature")
        .fields()
        .requiredDouble("temperature")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForLength extends SchemaFor[Length] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.space")
        .record("Length")
        .fields()
        .requiredDouble("length")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }

  implicit object SchemaForVolumeFlow extends SchemaFor[VolumeFlow] {
    override def schema: Schema =
      SchemaBuilder
        .builder("osquants.motion")
        .record("Volumeflow")
        .fields()
        .requiredDouble("volumeflow")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForVolume extends SchemaFor[Volume] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.space")
        .record("Volume")
        .fields()
        .requiredDouble("volume")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }

  implicit object SchemaForFrequency extends SchemaFor[Frequency] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.time")
        .record("Frequency")
        .fields()
        .requiredDouble("frequency")
        .requiredString("unit")
        .endRecord()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit object SchemaForVelocity extends SchemaFor[Velocity] {
    override def schema: Schema =
      SchemaBuilder
        .builder("squants.motion")
        .record("Velocity")
        .fields()
        .requiredDouble("velocity")
        .requiredString("unit")
        .endRecord()

    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }


  implicit object TimeEncoder extends Encoder[Time] {
    override def encode(t: Time): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("time", t.value)
      record.put("unit", t.unit.symbol)
      record
    }
    override def schemaFor: SchemaFor[Time] = SchemaForTime

  }

  implicit object TimeDecoder extends Decoder[Time] {
    override def decode(value: Any): Time = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("time").toString + " " + record.get("unit").toString
      Time(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Time"
        )
      )
    }
    override def schemaFor: SchemaFor[Time] = SchemaForTime
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

  implicit object VelocityEncoder extends Encoder[Velocity] {
    override def encode(t: Velocity): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put("velocity", t.value)
      record.put("unit", t.unit.symbol)
      record
    }

    override def schemaFor: SchemaFor[Velocity] = SchemaForVelocity
  }

  implicit object VelocityDecoder extends Decoder[Velocity] {
    override def decode(value: Any): Velocity = {
      val record = value.asInstanceOf[GenericRecord]
      val parsed = record.get("velocity").toString + " " + record.get("unit").toString
      Velocity(parsed).getOrElse(
        throw new IllegalStateException(
          s"AVRO failed to deserialize in radix code. $parsed is not a valid squants.Velocity"
        )
      )
    }

    override def schemaFor: SchemaFor[Velocity] = SchemaForVelocity
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

  class TimeAvro extends AvroSerializer[Time]
  class TemperatureAvro extends AvroSerializer[Temperature]
  class LengthAvro extends AvroSerializer[Length]
  class VolumeFlowAvro extends AvroSerializer[VolumeFlow]
  class VolumeAvro extends AvroSerializer[Volume]
  class FrequencyAvro extends AvroSerializer[Frequency]
  class VelocityAvro extends AvroSerializer[Velocity]

}
