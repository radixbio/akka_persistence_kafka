package com.radix.shared.persistence.serializations.device_drivers.eve

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.eve.defns.EveRequest

object actorInfo {
  val skey: ServiceKey[DriverRequest[EveRequest]] = ServiceKey[DriverRequest[EveRequest]]("eve-actor")
}
