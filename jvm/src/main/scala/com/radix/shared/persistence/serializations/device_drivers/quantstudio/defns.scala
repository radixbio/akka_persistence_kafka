package com.radix.shared.persistence.serializations.device_drivers.quantstudio

import akka.actor.typed.ActorRef
import com.radix.shared.persistence.serializations.device_drivers.quantstudio.defns.ReturnCode.ReturnCode
import java.nio.file.{Path => FilePath}

object defns {
  sealed trait QuantstudioRequest
  final case class RunQuaintStudioExperiment(experamentFile: FilePath) extends QuantstudioRequest
  final case class GetQuantStudioStatus(replyTo: ActorRef[ReturnCode]) extends QuantstudioRequest
  final case class LoadQuantStudioPlate( /*client: http4sMessagingClient[IO], url: String*/ ) extends QuantstudioRequest
  final case class RunExperimentAsync(experamentPath: FilePath, plateBarcode: String) extends QuantstudioRequest
  final case class StopExperiment() extends QuantstudioRequest

  object ReturnCode extends Enumeration {
    type ReturnCode = Int
    val UNINITIALIZED, SESSION_INVALID, SESSION_EXISTING, AUTHENTICATION_FAILURE, USER_ACCOUNT_DISABLED,
      PASSWORD_EXPIRED, INSTRUMENT_UNKNOWN, INSTRUMENT_NOT_CONNECTED, INSTRUMENT_READY, INSTRUMENT_RUNNING,
      INSTRUMENT_PAUSED, INSTRUMENT_IN_ERROR, INSTRUMENT_FAIL_TO_START, INSTRUMENT_TRAY_OPENED, INSTRUMENT_TRAY_CLOSED,
      EXPERIMENT_NOT_FOUND, EXPERIMENT_READ_ERROR, EXPERIMENT_WRITE_ERROR, EXPERIMENT_VALIDATION_ERROR = ReturnCode
  }
}
