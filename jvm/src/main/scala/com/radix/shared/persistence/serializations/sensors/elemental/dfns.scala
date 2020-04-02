package com.radix.shared.persistence.serializations.sensors.elemental

import java.time.Instant
import java.util.UUID

import com.radix.shared.persistence.AvroSerializer
import squants.Temperature
import com.radix.shared.persistence.serializations.squants.schemas._

object dfns {
  case class EMUUID(uuid: UUID)


  trait sensorReading[T] {
    val sensorName: String
    val timestamp: Instant
    val sensorUUID: java.util.UUID
    val data: T
  }

  sealed trait ElementalSensor {
    val sensorName: String
    val timestamp: Instant
    val battery: Int
    val gateway: String
    val rssi: Int
  }

  case class ElementalSensorDetails(name: String, model: String, deviceUUID: EMUUID, serialID: String)
  case class ElementT(sensorName: String,
                      sensorUUID: UUID,
                      battery: Int,
                      gateway: String,
                      rssi: Int,
                      temperature: Temperature,
                      sample_epoch: Instant,
                     ) extends sensorReading[Temperature]
    with ElementalSensor {
    override val timestamp: Instant = sample_epoch
    override val data: Temperature  = temperature
  }

  case class ElementAData(humidity: Double, light: Double, pressure: Double, temperature: Temperature)

  case class ElementA(sensorName: String,
                      sensorUUID: UUID,
                      battery: Int,
                      gateway: String,
                      humidity: Double,
                      light: Double,
                      pressure: Double,
                      rssi: Int,
                      temperature: Temperature,
                      sample_epoch: Instant,
                     ) extends sensorReading[ElementAData]
    with ElementalSensor {
    override val timestamp: Instant = sample_epoch
    override val data               = ElementAData(humidity, light, pressure, temperature)
  }
  case class ThermocoupleUnplugged(sensorName: String,
                                   sensorUUID: UUID,
                                   battery: Int,
                                   gateway: String,
                                   rssi: Int,
                                   thermocouple_unplugged: Boolean,
                                   sample_epoch: Instant)
    extends sensorReading[Boolean]
      with ElementalSensor {
    override val timestamp: Instant = sample_epoch
    override val data: Boolean      = thermocouple_unplugged
  }
  class ElementAAvro  extends AvroSerializer[ElementA]
  class ElementTAvro  extends AvroSerializer[ElementT]
  class UnpluggedAvro extends AvroSerializer[ThermocoupleUnplugged]
}
