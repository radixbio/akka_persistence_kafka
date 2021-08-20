package com.radix.shared.persistence.serializations.device_drivers.octet

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.octet.defns.OctetRequest

object actorInfo {
  val serviceKey: ServiceKey[DriverRequest[OctetRequest]] = ServiceKey("octet")
}
