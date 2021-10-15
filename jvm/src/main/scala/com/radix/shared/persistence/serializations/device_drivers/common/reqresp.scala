package com.radix.shared.persistence.serializations.device_drivers.common

import scala.reflect.ClassTag

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._

object requests {
  import responses.CoreDriverResponse

  /**
   * A request which all drivers are equipped to handle
   */
  sealed trait CoreDriverRequest {
    def replyTo: Option[ActorRef[CoreDriverResponse]]
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
  implicit def lreqToReq[L](lreq: L): DriverRequest[L] = Local[L](lreq)

  case class HealthCheck(replyTo: Option[ActorRef[CoreDriverResponse]]) extends CoreDriverRequest

  class HealthCheckSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HealthCheck]
}

object responses {

  /**
   * A response which all drivers can send
   */
  sealed trait CoreDriverResponse

  case class HealthStatus(healthy: Boolean, status: String) extends CoreDriverResponse

  case class HealthStatusSerializer() extends AvroSerializer[HealthStatus]
}
