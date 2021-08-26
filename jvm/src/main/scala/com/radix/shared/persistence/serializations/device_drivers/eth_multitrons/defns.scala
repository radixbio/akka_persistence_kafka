package com.radix.shared.persistence.serializations.device_drivers.eth_multitrons

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import io.circe.Json
import squants.time.Frequency
import squants.thermal.Temperature

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.serializations.device_drivers.elemental.gateway.ElementalRadixDriverTypes._

object defns {
  type OReplyTo = Option[ActorRef[EMultitronsResponse]]

  sealed trait EMultitronsRequest
  sealed trait EMultitronsResponse {
    def asValueResp: Either[EMultitronsResponse, ValueResponse] = this match {
      case v: ValueResponse => Right(v)
      case _                => Left(this)
    }
  }
  sealed trait EMultitronsEvent

  sealed trait AbsoluteCommand extends EMultitronsRequest {
    val replyTo: OReplyTo
  }
  sealed trait WriteCommand extends AbsoluteCommand {
    val parameterID: Byte
    val setValue: Float
  }
  sealed trait ReadCommand extends AbsoluteCommand {
    val parameterID: Byte
  }

  sealed trait BundledCommand extends EMultitronsRequest {
    val replyTo: OReplyTo

    def getAbsolute: List[AbsoluteCommand]

    def reply(results: List[EMultitronsResponse], sender: ActorRef[_]): EMultitronsResponse
  }

  case class ReadShakerSpeed(replyTo: OReplyTo) extends ReadCommand {
    override val parameterID: Byte = 1
  }
  case class ReadTemperature(replyTo: OReplyTo) extends ReadCommand {
    override val parameterID: Byte = 0
  }
  case class ReadChamberHumidity(replyTo: OReplyTo) extends ReadCommand {
    override val parameterID: Byte = 3
  }
  case class ReadCO2Level(replyTo: OReplyTo) extends ReadCommand {
    override val parameterID: Byte = 2
  }
  case class ReadLightIntensity(replyTo: OReplyTo) extends ReadCommand {
    override val parameterID: Byte = 4
  }

  case class SetShakerSpeed(freq: Frequency, replyTo: OReplyTo) extends WriteCommand {
    override val parameterID: Byte = 1
    override val setValue: Float = codex.freqAsFloat(freq)
  }
  case class SetTemperature(temp: Temperature, replyTo: OReplyTo) extends WriteCommand {
    override val parameterID: Byte = 0
    override val setValue: Float = codex.tempAsFloat(temp)
  }
  case class SetChamberHumidity(percent: Float, replyTo: OReplyTo) extends WriteCommand {
    override val parameterID: Byte = 3
    override val setValue: Float = percent
  }
  case class SetCO2Level(percent: Float, replyTo: OReplyTo) extends WriteCommand {
    override val parameterID: Byte = 2
    override val setValue: Float = percent
  }
  case class SetLightIntensity(percent: Float, replyTo: OReplyTo) extends WriteCommand {
    override val parameterID: Byte = 4
    override val setValue: Float = percent
  }
//  case class SetLightValue(on: Boolean, replyTo: OReplyTo) extends WriteCommand {
//    override val parameterID: Byte = 6
//    override val setValue: Float = if (on) 1 else 0
//  }

  case class SummaryRequest(replyTo: OReplyTo) extends BundledCommand {
    override def getAbsolute: List[AbsoluteCommand] = List(
      ReadShakerSpeed(None),
      ReadTemperature(None),
      ReadChamberHumidity(None),
      ReadCO2Level(None),
      ReadLightIntensity(None)
    )

    override def reply(results: List[EMultitronsResponse], sender: ActorRef[_]): EMultitronsResponse = {
      val resp = if (results.length != 5) {
        ErrorOccurred("Did not receive all required summary components")
      } else {
        val irp = for {
          spd <- results.head.asValueResp.map(f => codex.floatAsFreq(f.value))
          tmp <- results(1).asValueResp.map(f => codex.floatAsTemp(f.value))
          hum <- results(2).asValueResp.map(_.value)
          co2 <- results(3).asValueResp.map(_.value)
          light <- results(4).asValueResp.map(_.value)
        } yield SummaryResponse(spd, tmp, hum, co2, light, sender.path.name)

        irp match {
          case Left(other) =>
            other match {
              case s: SummaryResponse => s
              case e: ErrorOccurred   => e
              case other              => ErrorOccurred(s"Improper response: $other")
            }
          case Right(value) => value
        }
      }

      replyTo.foreach(_ ! resp)

      resp
    }
  }

  case class SummaryResponse(
    shakerSpeed: Frequency,
    temperature: Temperature,
    chamberHumidity: Float,
    co2Level: Float,
    lightIntensity: Float,
    actorID: String
  ) extends EMultitronsResponse
      with ElementalSendable
      with ElementalDriverable {

    override def packets: List[ElementalDriverable] = List(this)

    override def toElementalJSON: Json = Json.obj(
      "shaker_speed" -> Json.obj(
        "value" -> Json.fromDouble(shakerSpeed.toRevolutionsPerMinute).get,
        "unit" -> Json.fromString("rpm")
      ),
      "temperature" -> Json.obj(
        "value" -> Json.fromDouble(temperature.toCelsiusDegrees).get,
        "unit" -> Json.fromString("celsius")
      ),
      "chamber_humidity" -> Json.fromFloat(chamberHumidity).get,
      "co2_level" -> Json.fromFloat(co2Level).get,
      "light_intensity" -> Json.fromFloat(lightIntensity).get
    )

    override def uidPostfix: String = actorID
  }

  case class ErrorOccurred(cause: String) extends EMultitronsResponse with EMultitronsEvent

  case class ValueResponse(value: Float) extends EMultitronsResponse
  case class WriteConfirmation() extends EMultitronsResponse

  case class CommandResponse(cmd: EMultitronsRequest, rsp: EMultitronsResponse) extends EMultitronsEvent

  class ReadShakerSpeedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadShakerSpeed]
  class ReadTemperatureAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadTemperature]
  class ReadChamberHumidityAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadChamberHumidity]
  class ReadCO2LevelAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadCO2Level]
  class ReadLightIntensityAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[ReadLightIntensity]
  class SetShakerSpeedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetShakerSpeed]
  class SetTemperatureAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetTemperature]
  class SetChamberHumidityAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetChamberHumidity]
  class SetCO2LevelAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetCO2Level]
  class SetLightIntensityAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetLightIntensity]
  class SummaryRequestAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[SummaryRequest]
  class SummaryResponseAvro extends AvroSerializer[SummaryResponse]
  class ErrorOccurredAvro extends AvroSerializer[ErrorOccurred]
  class ValueResponseAvro extends AvroSerializer[ValueResponse]
  class WriteConfirmationAvro extends AvroSerializer[WriteConfirmation]
  class CommandResponseAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandResponse]
}
