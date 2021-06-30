package com.radix.shared.persistence.serializations.device_drivers.omnidriver

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.omnidriver.defns.OmnidriverRequest

object actorInfo {
  val skey: ServiceKey[DriverRequest[OmnidriverRequest]] =
    ServiceKey[DriverRequest[OmnidriverRequest]]("omnidriver")
}
