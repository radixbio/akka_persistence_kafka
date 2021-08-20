package com.radix.shared.persistence.serializations.device_drivers.octet

import java.time.Instant

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import squants.Temperature
import squants.Length
import squants.time.{Frequency, Time}
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.serializations.squants.Serializers._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

object defns {
  sealed trait OctetRequest

  sealed trait ThermalControlCommand extends OctetRequest {val replyTo:Option[ActorRef[ControlResponse]]}
  case class SetPlateTemperature(target: Temperature, replyTo: Option[ActorRef[ControlResponse]])
    extends ThermalControlCommand
  case class DisablePlateHeating(replyTo: Option[ActorRef[ControlResponse]]) extends ThermalControlCommand

  sealed trait ShakerControlCommand extends OctetRequest {val replyTo:Option[ActorRef[ControlResponse]]}
  case class StartShaker(velocity: Frequency, accel: Time, clockwise: Boolean,
                         replyTo: Option[ActorRef[ControlResponse]]) extends ShakerControlCommand
  case class StopShaker(home: Boolean, replyTo: Option[ActorRef[ControlResponse]]) extends ShakerControlCommand
  case class MoveShakerHome(replyTo: Option[ActorRef[ControlResponse]]) extends ShakerControlCommand

  sealed trait MotionControlCommand extends OctetRequest {val replyTo:Option[ActorRef[ControlResponse]]}
  case class MoveSensorHome(replyTo: Option[ActorRef[ControlResponse]]) extends MotionControlCommand
  case class PickupSensor(row: Int, replyTo: Option[ActorRef[ControlResponse]]) extends MotionControlCommand
  case class EjectSensor(replyTo: Option[ActorRef[ControlResponse]]) extends MotionControlCommand
  case class MoveToSample(row: Int, replyTo: Option[ActorRef[ControlResponse]]) extends MotionControlCommand

  sealed trait SensorControlCommand extends OctetRequest {val replyTo:Option[ActorRef[ControlResponse]]}
  sealed trait SpectrophotometryCommand extends OctetRequest { val replyTo: Option[ActorRef[ScanResponse]] }
  case class ModifySensorParameters(lampOn: Option[Boolean], integrationTime: Option[Time],
                                    replyTo: Option[ActorRef[ControlResponse]]) extends SensorControlCommand

  case class GetSpectrum(replyTo: Option[ActorRef[ScanResponse]]) extends SpectrophotometryCommand

  case class PerformScan(sampleRowNum: Int,
                         sensorRowNum: Int,
                         samplingTime: Time,
                         contentsMap: Map[String, Option[String]],
                         numChannels: Int,
                         targetTemperature: Temperature,
                         // shake frequency, acceleration time, clockwise rotation?
                         shakerConfig: (Frequency, Time, Boolean),
                         // integration time, lampOn?
                         sensorConfig: (Time, Option[Boolean]),
                         replyTo: Option[ActorRef[OctetResponse]]) extends OctetRequest

  case class PerformScanHttp(sampleRowNum: Int,
                             sensorRowNum: Int,
                             samplingTime: Time,
                             contentsMap: Map[String, Option[String]],
                             numChannels: Int,
                             targetTemperature: Temperature,
                             // shake frequency, acceleration time, clockwise rotation?
                             shakerConfig: (Frequency, Time, Boolean),
                             // integration time, lampOn?
                             sensorConfig: (Time, Option[Boolean])) extends OctetRequest {
    def toActorified(actorRef: ActorRef[OctetResponse]): PerformScan = {
      PerformScan(
        sampleRowNum,
        sensorRowNum,
        samplingTime,
        contentsMap,
        numChannels,
        targetTemperature,
        shakerConfig,
        sensorConfig,
        Some(actorRef)
      )
    }

    def toActorified: PerformScan = {
      PerformScan(
        sampleRowNum,
        sensorRowNum,
        samplingTime,
        contentsMap,
        numChannels,
        targetTemperature,
        shakerConfig,
        sensorConfig,
        None
      )
    }
  }

  sealed trait OctetResponse {val success:Boolean; val message:String}

  sealed trait ControlResponse extends OctetResponse
  case class ControlCommandFailed(message: String) extends ControlResponse {val success:Boolean=false}
  case class ControlSucceeded() extends ControlResponse {val success:Boolean=true; val message:String=""}

  sealed trait ScanResponse extends OctetResponse
  case class ScanSuccessResponse(spectrum: Array[Double], saturated: Boolean, wavelength: Array[Length],
                          timestamp: Instant, channel:Int) extends ScanResponse {
    override val message: String = ""
    override val success: Boolean = true

    def toCSVRow(contentMap: Map[Int, String]): String = {
      val ts = timestamp.toString
      s"$ts;$saturated;${contentMap(channel)};${spectrum.mkString(",")};${wavelength.mkString(",")}"
    }
  }
  case class ScanFailedResponse(error: String) extends ScanResponse {
    override val success: Boolean = false
    override val message: String = error
  }
  case class AnalysisSuccessResponse() extends OctetResponse {
    val success = true
    override val message: String = ""
  }
  case class AnalysisFailureResponse(error: String) extends OctetResponse {
    override val success: Boolean = false
    override val message: String = error
  }

  implicit val performScanHttpEncoder: Encoder[PerformScanHttp] = deriveEncoder[PerformScanHttp]
  implicit val performScanHttpDecoder: Decoder[PerformScanHttp] = deriveDecoder[PerformScanHttp]

  sealed trait OctetEvent
  case class ExecutedCommand(cmd: OctetRequest) extends OctetEvent
  case class CommandFailed(cmd: OctetRequest, cause: String) extends OctetEvent

  class ScanSuccessResponseSerializer extends AvroSerializer[ScanSuccessResponse]
  class ScanFailedResponseSerializer extends AvroSerializer[ScanFailedResponse]
  class SetPlateTemperatureSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetPlateTemperature]
  class DisablePlateHeatingSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[DisablePlateHeating]
  class StartShakerSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[StartShaker]
  class StopShakerSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[StopShaker]
  class MoveShakerHomeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[MoveShakerHome]
  class MoveSensorHomeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[MoveSensorHome]
  class PickupSensorSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[PickupSensor]
  class EjectSensorSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[EjectSensor]
  class MoveToSampleSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[MoveToSample]
  class ModifySensorParametersSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ModifySensorParameters]
  class PerformScanSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[PerformScan]
  class GetSpectrumSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetSpectrum]

  class AnalysisSuccessResponseSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[AnalysisSuccessResponse]
  class AnalysisFailureResponseSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[AnalysisFailureResponse]

  class OctetRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetRequest]

  class ControlSucceededSerializer extends AvroSerializer[ControlSucceeded]
  class ControlCommandFailedSerializer extends AvroSerializer[ControlCommandFailed]

  class ExecutedCommandSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ExecutedCommand]
  class CommandFailedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandFailed]
}
