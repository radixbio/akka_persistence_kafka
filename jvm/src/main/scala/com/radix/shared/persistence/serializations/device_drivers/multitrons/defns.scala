package com.radix.shared.persistence.serializations.device_drivers.multitrons

import java.time.Instant

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import io.circe.Json
import scodec.bits.BitVector

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.device_drivers.common.responses.DriverResponse
import com.radix.shared.persistence.serializations.device_drivers.elemental_radix_driver.ElementalRadixDriverTypes._

object defns {

  sealed trait MultitronsUnit {
    val addressOffset: Int
    val uuidDefaultPostfix: String
  }

  case class MultitronsUpper() extends MultitronsUnit {
    override val addressOffset: Int = 0
    override val uuidDefaultPostfix: String = "-A"
  }

  case class MultitronsMiddle() extends MultitronsUnit {
    override val addressOffset: Int = 16
    override val uuidDefaultPostfix: String = "-B"
  }

  case class MultitronsLower() extends MultitronsUnit {
    override val addressOffset: Int = 32
    override val uuidDefaultPostfix: String = "-C"
  }

  type ReplyToActor = ActorRef[DriverResponse[MultitronsResponse]]

  sealed trait MultitronsRequest {
    def replyTo: Option[ReplyToActor]
  }

  case class SummaryRequest(replyTo: Option[ReplyToActor]) extends MultitronsRequest

  case class SingleSummaryRequest(replyTo: Some[ReplyToActor], unit: MultitronsUnit)
    extends MultitronsRequest

  case class CheckConnectedRequest(unit: MultitronsUnit, replyTo: Some[ReplyToActor]) extends MultitronsRequest

  sealed trait MultitronsCommand extends MultitronsRequest {
    override def replyTo: Option[ReplyToActor]

    def cmd: BitVector

    def unit: MultitronsUnit
  }

  /**
   * Creates a read command for a specific value from Multitron machine.
   *
   * @param commandId numerical id of the specific sensor
   * @param unit      multitron unit
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
   * @param unit      multitron unit
   * @param setValue  value to be written
   * @return write command
   */
  def writeCommand(commandId: Int, unit: MultitronsUnit, setValue: Int): BitVector = {
    BitVector(List(2, 48, 48, commandId + unit.addressOffset, 82, 67, setValue, 3).map(_.toByte))
  }

  case class ReadTemperature(unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector = {
      readCommand(128, unit)
    }
  }

  case class ReadSpeed(unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      readCommand(129, unit)
  }

  case class ReadLights(unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      readCommand(130, unit)
  }

  case class ReadHumidity(unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      readCommand(131, unit)
  }

  case class ReadCO2(unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      readCommand(132, unit)
  }

  case class WriteTemperature(setTmp: Int, unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      writeCommand(128, unit, setTmp)
  }

  case class WriteSpeed(setSpeed: Int, unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      writeCommand(129, unit, setSpeed)
  }

  case class WriteLights(setLights: Boolean, unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    private[multitrons] implicit def boolToInt(boo: Boolean): Int = if (boo) 1 else 0

    override val cmd: BitVector =
      writeCommand(130, unit, setLights)
  }

  case class WriteHumidity(setHumidity: Int, unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      writeCommand(131, unit, setHumidity)
  }

  case class WriteCO2(setCO2: Int, unit: MultitronsUnit, replyTo: Option[ReplyToActor]) extends MultitronsCommand {
    override val cmd: BitVector =
      writeCommand(132, unit, setCO2)
  }

  case class SerialMessage(value: String, unit: String, deviceID: String)

  object SerialMessage {
    def byteMe(bytes: Seq[Byte]): String = {
      bytes.map(f => f.toChar).mkString
    }

    def apply(rsp: Seq[Byte], subId: Byte, devID: Seq[Byte]): SerialMessage = {
      val suid = (subId.toInt + 256).toHexString
      val did = byteMe(devID)
      val r = byteMe(rsp)

      new SerialMessage(r, suid, did)
    }
  }

  case class SummaryUnit(co2: Option[String],
                    humidity: Option[String],
                    temperature: Option[String],
                    lights: Option[String],
                    speed: Option[String],
                    uidadd: String,
                    time: Long) extends ElementalDriverable {

    override def uidPostfix: String = uidadd

    def scale(in: Option[String]): Option[String] = {
      in match {
        case Some(s) =>
          val num = s.replace(" ", "")
          Some((num.toInt / 10.0).toString)
        case None => None
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
        "timestamp" -> Some(this.time.toString)
      )

      // Elemental can't handle/filter error responses, so we have to do it for them.
      val sansErrors = forjson.filter(p => p._2.isDefined)

      val json = sansErrors.map(p => Tuple2(p._1, Json.fromString(p._2.get)))

      Json.fromFields(json.toList)
    }

  }

  object SummaryUnit {
    implicit def smToStr(o: Option[SerialMessage]): Option[String] = o match {
      case None => None
      case Some(value) => Some(value.value)
    }

    def apply(
               co2: Option[SerialMessage],
               humidity: Option[SerialMessage],
               temperature: Option[SerialMessage],
               lights: Option[SerialMessage],
               speed: Option[SerialMessage],
               uidadd: String
             ): SummaryUnit =
      new SummaryUnit(co2, humidity, temperature, lights, speed, uidadd, Instant.now().getEpochSecond)
  }

  sealed trait MultitronsResponse

  case class ValueResponse(value: Option[SerialMessage]) extends MultitronsResponse
  case class SummaryResponse(upper: SummaryUnit, middle: SummaryUnit, lower: SummaryUnit)
    extends MultitronsResponse with ElementalSendable {
    override def packets: List[ElementalDriverable] = List(upper,middle,lower)
  }
  case class SingleSummaryResponse(value: SummaryUnit)
    extends MultitronsResponse with ElementalSendable {
    override def packets: List[ElementalDriverable] = List(value)
  }

  sealed trait ConnectivityCheckResponse extends MultitronsResponse
  case class UnitConnected() extends ConnectivityCheckResponse
  case class UnitNotConnected() extends ConnectivityCheckResponse

  case class ReadResult(result: Option[SerialMessage], attempts: Int) {
    def flawless: Boolean = result.isDefined && attempts <= 1
  }

  case class SummaryReadResult(co2: ReadResult, humidity: ReadResult, temperature: ReadResult,
                               lights: ReadResult, speed: ReadResult) {
    def flawless: Boolean = {
      co2.flawless && humidity.flawless && temperature.flawless && lights.flawless && speed.flawless
    }
  }

  sealed trait MultitronsEvent

  case class CommandProcessed(cmd: MultitronsCommand, result: ReadResult) extends MultitronsEvent
  case class SummaryGenerated(upper: SummaryReadResult, middle: SummaryReadResult, lower: SummaryReadResult) extends MultitronsEvent
  case class SingleSummaryGenerated(result: SummaryReadResult) extends MultitronsEvent
  case class ConnectivityExamined(unit: MultitronsUnit, result: ConnectivityCheckResponse) extends MultitronsEvent

  class MultitronsLowerSerializer extends AvroSerializer[MultitronsLower]
  class MultitronsMiddleSerializer extends AvroSerializer[MultitronsMiddle]
  class MultitronsUpperSerializer extends AvroSerializer[MultitronsUpper]

  class CheckConnectedRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CheckConnectedRequest]
  class CommandProcessedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandProcessed]
  class ConnectivityExaminedSerializer extends AvroSerializer[ConnectivityExamined]
  class ReadCO2Serializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadCO2]
  class ReadHumiditySerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadHumidity]
  class ReadLightsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadLights]
  class ReadSpeedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadSpeed]
  class ReadTemperatureSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadTemperature]
  class ReadResultSerializer extends AvroSerializer[ReadResult]

  class SerialMessageSerializer extends AvroSerializer[SerialMessage]
  class SingleSummaryGeneratedSerializer extends AvroSerializer[SingleSummaryGenerated]
  class SingleSummaryRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SingleSummaryRequest]
  class SingleSummaryResponseSerializer extends AvroSerializer[SingleSummaryResponse]
  class SummaryGeneratedSerializer extends AvroSerializer[SummaryGenerated]
  class SummaryReadResultSerializer extends AvroSerializer[SummaryReadResult]
  class SummaryRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SummaryRequest]
  class SummaryResponseSerializer extends AvroSerializer[SummaryResponse]
  class SummaryUnitSerializer extends AvroSerializer[SummaryUnit]
  class UnitConnectedSerializer extends AvroSerializer[UnitConnected]
  class UnitNotConnectedSerializer extends AvroSerializer[UnitNotConnected]
  class ValueResponseSerializer extends AvroSerializer[ValueResponse]
  class WriteCO2Serializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[WriteCO2]
  class WriteHumiditySerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[WriteHumidity]
  class WriteLightsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[WriteLights]
  class WriteSpeedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[WriteSpeed]
  class WriteTemperatureSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[WriteTemperature]
}
