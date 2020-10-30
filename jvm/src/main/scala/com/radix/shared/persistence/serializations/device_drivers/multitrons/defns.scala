package com.radix.shared.persistence.serializations.device_drivers.multitrons

import java.time.Instant

import akka.actor.{ActorLogging, ActorRef, ExtendedActorSystem}
import akka.event.LoggingAdapter
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.device_drivers.elemental_radix_driver.ElementalRadixDriverTypes.{ElementalSendable, ElemetalDriverable}
import scodec.bits.BitVector
import io.circe.Json

object defns {

  /**
   * The multitrons can be a one unit or three unit stack. This is how the internal docs talk about them. The different
   * units have different addresses for the serial communication protocol. Note that the units' addresses are not
   * necessarily the same as their order in a stack.
   *
   * The protocol mixes encodings, we we're just using an offset to work all in base ten here. That's the addressOffset.
   */
  sealed trait MultitronsUnit {
    val addressOffset: Int
    val uuidDefaultPostfix: String
  }

  case object MultitronsUpper extends MultitronsUnit {
    override val addressOffset: Int = 0
    override val uuidDefaultPostfix: String = "-A"
  }

  case object MultitronsMiddle extends MultitronsUnit {
    override val addressOffset: Int = 16
    override val uuidDefaultPostfix: String = "-B"
  }

  case object MultitronsLower extends MultitronsUnit {
    override val addressOffset: Int = 32
    override val uuidDefaultPostfix: String = "-C"
  }

  sealed trait MultitronsActorResponse
  sealed trait MultitronsActorRequest
  case class MultitronsSummaryRequest(respondTo: ActorRef) extends MultitronsActorRequest
  case class MultitronsSummaryResponse(
    aUpper: MultitronsSummaryUnit,
    bMiddle: MultitronsSummaryUnit,
    cLower: MultitronsSummaryUnit
  ) extends MultitronsActorResponse
      with ElementalSendable {
    override def packets = List(aUpper, bMiddle, cLower)
  }
  case class MultitronsSingleSummaryRequest(respondTo: ActorRef, unit: MultitronsUnit) extends MultitronsActorRequest
  case class CheckMultitronsConnected(unit: MultitronsUnit) extends MultitronsActorRequest
  sealed trait MultitronsZombieCheckRsp extends MultitronsActorResponse
  case object UnitResponsive extends MultitronsZombieCheckRsp
  case object UnitUnresponsive extends MultitronsZombieCheckRsp

  case class MultitronsSingleSummaryResponse(unit: MultitronsSummaryUnit)
      extends MultitronsActorResponse
      with ElementalSendable {
    override def packets = List(unit)
  }

  case class MultitronsSingleRetryMetrics(unit: MultitronsRetryReadMetrics) extends ElementalSendable {
    override def packets = List(unit)
  }
  case class MultitronsRetryReadMetrics(
    co2: Int,
    humidity: Int,
    temperature: Int,
    lights: Int,
    speed: Int,
    uidadd: String,
    time: Instant
  ) extends ElemetalDriverable {
    override def toElementalJSON: Json = {
      val forjson: Map[String, String] = Map[String, String](
        "co2_num_read_attempts" -> co2.toString,
        "humidity_num_read_attempts" -> humidity.toString,
        "temperature_num_read_attempts" -> temperature.toString,
        "lights_num_read_attempts" -> lights.toString,
        "speed_num_read_attempts" -> speed.toString,
        "mac_address" -> (uuid + this.uidadd),
        "timestamp" -> this.time.getEpochSecond.toString
      )

      val json = forjson.map(p => Tuple2(p._1, Json.fromString(p._2)))

      Json.fromFields(json.toList)
    }

    override def uidPostfix: String = this.uidadd
  }
  object MultitronsRetryReadMetrics {
    def apply(
      co2: Int,
      humidity: Int,
      temperature: Int,
      lights: Int,
      speed: Int,
      uidadd: String
    ): MultitronsRetryReadMetrics = {
      MultitronsRetryReadMetrics(co2, humidity, temperature, lights, speed, uidadd, Instant.now())
    }
  }

  case class MultitronsSerialResp(resp: String, unit: String, deviceID: String)
  object MultitronsSerialResp {
    def byteMe(bytes: Seq[Byte]): String = {
      bytes.map(f => f.toChar).mkString
    }
    def apply(rsp: Seq[Byte], subId: Byte, devID: Seq[Byte])(implicit log: LoggingAdapter): MultitronsSerialResp = {
      val suid = (subId.toInt + 256).toHexString
      val did = byteMe(devID)
      val r = byteMe(rsp)

      log.debug(
        s"\nMain ID: $did\n Sub ID (Hex): $suid (Int): $subId\n  Data: $r \n"
      )

      new MultitronsSerialResp(did, suid, r)
    }
  }

  object MultitronsSummaryUnit {
    def apply(
      co2: Option[MultitronsSerialResp],
      humidity: Option[MultitronsSerialResp],
      temperature: Option[MultitronsSerialResp],
      lights: Option[MultitronsSerialResp],
      speed: Option[MultitronsSerialResp],
      uidadd: String
    ): MultitronsSummaryUnit = {
      implicit def fuckItGetTheThing(o: Option[MultitronsSerialResp]): Option[String] = {
        o match {
          case None        => None
          case Some(value) => Some(value.resp)
        }
      }

      MultitronsSummaryUnit(co2, humidity, temperature, lights, speed, uidadd, Instant.now())
    }
  }
  case class MultitronsSummaryUnit(
    co2: Option[String],
    humidity: Option[String],
    temperature: Option[String],
    lights: Option[String],
    speed: Option[String],
    uidadd: String,
    time: Instant
  ) extends ElemetalDriverable {

    override def uidPostfix: String = uidadd

    def scale(in: Option[String]): Option[String] = {
      in match {
        case Some(s) => {
          val num = s.replace(" ", "")
          Some((num.toInt / 10.0).toString)
        }
        case None => {
          None
        }
      }
    }

    override def toElementalJSON: Json = {
      val forjson: Map[String, Option[String]] = Map[String, Option[String]](
        "co2" -> scale(co2),
        "humidity" -> scale(humidity),
        "temperature" -> scale(temperature),
        "lights" -> lights,
        "speed" -> speed,
        "mac_address" -> Some(uuid + this.uidadd),
        "timestamp" -> Some(this.time.getEpochSecond.toString)
      )

      // Elemental can't handle/filter error responses, so we have to do it for them.
      val sansErrors = forjson.filter(p => p._2.isDefined)

      val json = sansErrors.map(p => Tuple2(p._1, Json.fromString(p._2.get)))

      Json.fromFields(json.toList)
    }
  }

  case class MultitronsValue(value: Option[String]) extends MultitronsActorResponse

  /**
   * DSL for communicating with the Multitrons. Generally, you're picking:
   *  --which unit (upper, middle, or lower)
   *  --which value (co2, humidity, lights, speed, or temperature)
   *  --if you want to read the value or set it
   * KEEP IN MIND: if you write a parameter, it will change the value of the machine. The the incubator is in use, this is
   * a problem. DO NOT set parameters on customer production devices without talking to them first.
   */
  /**
   * This is how we tell the device what to do and how we encode the serial protocol. We can read and set the Temperature,
   * Speed, Lights, Humidity, and CO2. Note that with reading from the device, we can't directly read from the serial
   * port. This can only be done with a callback method. Therefore we maintain state of the previous command set. We we
   * were to give the device commands too quickly, weird serial behavior could happen. However, the hardware would
   * probably get grumpy before the serial connection. I'd say don't send commands faster than every second or two which
   * is much faster than we need in real life.
   */
  sealed trait MultitronsCommand {
    val command: BitVector
    val unit: MultitronsUnit
  }

  /**
   * Creates a read command for a specific value from Multitron machine.
   *
   * @param commandId numerical id of the specific sensor
   * @param unit multitron unit
   * @return read command to get value from a specific sensor of a multitron
   */
  def readCommand(commandId: Int, unit: MultitronsUnit): BitVector = {
    BitVector(List(2, 48, 48, commandId + unit.addressOffset, 82, 65, 3).map(_.toByte))
  }

  /**
   * Creates a write command for a specific type of value (commandId) for a multitron unit,
   * writing value setValue.
   *
   * @param commandId id of the value to write
   * @param unit multitron unit
   * @param setValue value to be written
   * @return write command
   */
  def writeCommand(commandId: Int, unit: MultitronsUnit, setValue: Int): BitVector = {
    BitVector(List(2, 48, 48, commandId + unit.addressOffset, 82, 67, setValue, 3).map(_.toByte))
  }

  case class ReadTemperature(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector = {
      readCommand(128, unit)
    }
  }

  case class ReadSpeed(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(129, unit)
  }

  case class ReadLights(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(130, unit)
  }

  case class ReadHumidity(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(131, unit)
  }

  case class ReadCO2(unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      readCommand(132, unit)
  }

  case class WriteTemperature(setTmp: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(128, unit, setTmp)
  }

  case class WriteSpeed(setSpeed: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(129, unit, setSpeed)
  }

  case class WriteLights(setLights: Boolean, unit: MultitronsUnit) extends MultitronsCommand {
    private[multitrons] implicit def boolToInt(boo: Boolean): Int = {
      boo match {
        case true  => 1
        case false => 0
      }
    }

    override val command: BitVector =
      writeCommand(130, unit, setLights)
  }

  case class WriteHumidity(setHumidity: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(131, unit, setHumidity)
  }

  case class WriteCO2(setCO2: Int, unit: MultitronsUnit) extends MultitronsCommand {
    override val command: BitVector =
      writeCommand(132, unit, setCO2)
  }

  class MultitronsSummaryUnitSerializer extends AvroSerializer[MultitronsSummaryUnit]
  class MultitronsSummaryResponseSerializer extends AvroSerializer[MultitronsSummaryResponse]
  class MultitronsSummaryRequestSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[MultitronsSummaryRequest]

  class ReadTemperatureSerializer extends AvroSerializer[ReadTemperature]
  class ReadSpeedSerializer extends AvroSerializer[ReadSpeed]
  class ReadCO2Serializer extends AvroSerializer[ReadCO2]
  class ReadHumiditySerializer extends AvroSerializer[ReadHumidity]
  class ReadLightsSerializer extends AvroSerializer[ReadLights]

  class WriteTemperatureSerializer extends AvroSerializer[WriteTemperature]
  class WriteSpeedSerializer extends AvroSerializer[WriteSpeed]
  class WriteCO2Serializer extends AvroSerializer[WriteCO2]
  class WriteHumiditySerializer extends AvroSerializer[WriteHumidity]
  class WriteLightsSerializer extends AvroSerializer[WriteLights]
}
