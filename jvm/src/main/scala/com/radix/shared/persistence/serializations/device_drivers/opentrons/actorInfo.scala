package com.radix.shared.persistence.serializations.device_drivers.opentrons

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.opentrons.defns.OpentronsRequest

object actorInfo {
  val skey = ServiceKey[OpentronsRequest]("opentrons-actor")
}
