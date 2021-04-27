package com.radix.shared.persistence.serializations.device_drivers.quantstudio

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.quantstudio.defns.{AutomationServiceRequest, QuantstudioRequest}

object actorInfo {
  val skey = ServiceKey[DriverRequest[QuantstudioRequest]]("quantstudio-driver")
  val asServiceKey = ServiceKey[DriverRequest[AutomationServiceRequest]]("quantstudio-automation-server")
}
