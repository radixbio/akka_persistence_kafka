package com.radix.shared.persistence.serializations.device_drivers.ht91100

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.ht91100.defns.Ht91100Request

object actorInfo {
  val skey: ServiceKey[DriverRequest[Ht91100Request]] = ServiceKey[DriverRequest[Ht91100Request]]("ht91100")
}
