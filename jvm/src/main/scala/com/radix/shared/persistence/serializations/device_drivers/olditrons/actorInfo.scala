package com.radix.shared.persistence.serializations.device_drivers.olditrons

import akka.actor.typed.receptionist.ServiceKey
import scala.reflect.ClassTag

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.olditrons.defns.OlditronsRequest

object actorInfo {
  val serviceKey: ServiceKey[DriverRequest[OlditronsRequest]] = ServiceKey("olditrons-driver")
}
