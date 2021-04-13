package com.radix.shared.persistence.serializations.device_drivers.multitrons

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.multitrons.defns.MultitronsRequest

object actorInfo {
  val serviceKey: ServiceKey[DriverRequest[MultitronsRequest]] = ServiceKey("multitrons-driver")
}
