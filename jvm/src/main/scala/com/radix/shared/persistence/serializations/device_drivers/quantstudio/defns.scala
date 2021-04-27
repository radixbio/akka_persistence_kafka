package com.radix.shared.persistence.serializations.device_drivers.quantstudio

import akka.actor.typed.ActorRef
import java.nio.file.{Path => FilePath}

object defns {
  sealed trait QuantstudioRequest
  final case class RunQuaintStudioExperiment(experimentFile: FilePath) extends QuantstudioRequest
  final case class GetQuantStudioStatus(replyTo: Option[ActorRef[QSReturnCode]]) extends QuantstudioRequest
  final case class LoadQuantStudioPlate( /*client: http4sMessagingClient[IO], url: String*/ ) extends QuantstudioRequest
  final case class RunExperimentAsync(experimentPath: FilePath, plateBarcode: String) extends QuantstudioRequest
  final case class StopExperiment() extends QuantstudioRequest

  case class QSReturnCode(status: String, code: Int) {
    def good: Boolean =
      (code >= 8 && code <= 10) || code == 13 || code == 14
  }

  sealed trait AutomationServiceRequest
  case class RequestIP(replyTo: ActorRef[IPResult]) extends AutomationServiceRequest
  case class ProcessTerminated(code: Int) extends AutomationServiceRequest
  case class IPResult(ip: String)

  object QSReturnCode {
    val statusMapping: List[String] = List(
      "uninitialized",
      "session_invalid",
      "session_existing",
      "authentication_failure",
      "user_account_disabled",
      "password_expired",
      "instrument_unknown",
      "instrument_not_connected",
      "instrument_ready",
      "instrument_running",
      "instrument_paused",
      "instrument_in_error",
      "instrument_fail_to_start",
      "instrument_tray_opened",
      "instrument_tray_closed",
      "experiment_not_found",
      "experiment_read_error",
      "experiment_write_error",
      "experiment_validation_error"
    )

    def apply(code: Int): QSReturnCode = new QSReturnCode(statusMapping(code), code)

    val uninitialized = 0
    val sessionInvalid = 1
    val sessionExisting = 2
    val authenticationFailure = 3
    val userAccountDisabled = 4
    val passwordExpired = 5
    val instrumentUnknown = 6
    val instrumentNotConnected = 7
    val instrumentReady = 8
    val instrumentRunning = 9
    val instrumentPaused = 10
    val instrumentInError = 11
    val instrumentFailToStart = 12
    val instrumentTrayOpened = 13
    val instrumentTrayClosed = 14
    val experimentNotFound = 15
    val experimentReadError = 16
    val experimentWriteError = 17
    val experimentValidationError = 18
  }

  sealed trait QSEvent
  case class StartedExperiment(experimentFile: FilePath, plateBarcode: String) extends QSEvent
  case class StoppedExperiment() extends QSEvent
  case class DeviceStatusUpdated(status: Int) extends QSEvent
  case class LoadedPlate() extends QSEvent
  case class ErrorOccurred(message: String) extends QSEvent
}
