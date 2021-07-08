package com.radix.shared.persistence.serializations.device_drivers.opentrons

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.opentrons.defns.OpentronsRequest

object actorInfo {
  val skey = ServiceKey[DriverRequest[OpentronsRequest]]("opentrons-actor")
}
