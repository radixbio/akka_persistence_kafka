package com.radix.shared.persistence.serializations.device_drivers.watlow96_thermal

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.watlow96_thermal.defns.Watlow96Request

object actorInfo {
  val serviceKey: ServiceKey[DriverRequest[Watlow96Request]] = ServiceKey("watlow96_thermal")
}
