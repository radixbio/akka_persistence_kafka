package com.radix.shared.persistence.serializations.device_drivers.tecan

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.tecan.defns.TecanRequest

object actorInfo {
  val serviceKey: ServiceKey[DriverRequest[TecanRequest]] = ServiceKey("tecan-driver")
}
