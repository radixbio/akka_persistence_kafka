package com.radix.shared.persistence.serializations.device_drivers.eth_multitrons

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.eth_multitrons.defns.EMultitronsRequest

object actorInfo {
  val skey: ServiceKey[DriverRequest[EMultitronsRequest]] = ServiceKey("eth_multitrons")
}
