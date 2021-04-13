package com.radix.shared.persistence.serializations.device_drivers.common

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.device_drivers.common.responses.CoreDriverResponse

object requests {
  import responses.DriverResponse

  trait RequestWithReply[LR] {
    val replyTo: Option[ActorRef[DriverResponse[LR]]]
  }

  /**
   * A request which all drivers are equipped to handle
   */
  sealed trait CoreDriverRequest {
    def replyTo: Option[ActorRef[DriverResponse[_]]]

    def reply(msg: CoreDriverResponse): Unit = {
      replyTo match {
        case None => // Ignore
        case Some(actor) => actor ! responses.Core(msg)
      }
    }
  }

  /**
   * A generic wrapper which creates a union between CoreDriverRequests (`Core`) and
   * driver-specific commands (`Local`)
   * @tparam D The type of the driver-specific commands
   */
  sealed trait DriverRequest[D]
  case class Core[D](request: CoreDriverRequest) extends DriverRequest[D]
  case class Local[D](request: D) extends DriverRequest[D]

  implicit def creqToReq[L](creq: CoreDriverRequest): DriverRequest[L] = Core(creq)
  implicit def lreqToReq[L](lreq: L): DriverRequest[L] = Local(lreq)

  case class HealthCheck(replyTo: Option[ActorRef[DriverResponse[_]]]) extends CoreDriverRequest

  case class HealthCheckSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HealthCheck]
}

object responses {
  /**
   * A response which all drivers can send
   */
  sealed trait CoreDriverResponse

  /**
   * A generic wrapper which creates a union between CoreDriverResponses (`Core`) and
   * driver-specific responses (`Local`)
   * @tparam D The type of the driver-specific responses
   */
  sealed trait DriverResponse[D]
  case class Core[D](request: CoreDriverResponse) extends DriverResponse[D]
  case class Local[D](request: D) extends DriverResponse[D]

  implicit def crespToResp[L](cresp: CoreDriverResponse): DriverResponse[L] = Core(cresp)
  implicit def lrespToResp[L](lresp: L): DriverResponse[L] = Local(lresp)

  case class HealthStatus(healthy: Boolean, status: String) extends CoreDriverResponse

  case class HealthStatusSerializer() extends AvroSerializer[HealthStatus]
}
