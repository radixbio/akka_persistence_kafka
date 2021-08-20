package com.radix.shared.persistence.serializations.device_drivers.oceanoptics

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.oceanoptics.defns.OceanOpticsRequest

object actorInfo {
  val skey: ServiceKey[DriverRequest[OceanOpticsRequest]] =
    ServiceKey[DriverRequest[OceanOpticsRequest]]("oceanoptics")
}
