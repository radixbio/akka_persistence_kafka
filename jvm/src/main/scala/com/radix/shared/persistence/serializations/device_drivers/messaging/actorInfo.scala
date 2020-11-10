package com.radix.shared.persistence.serializations.device_drivers.messaging

import akka.actor.typed.receptionist.ServiceKey
import defns.Request

object actorInfo {
  val skey = ServiceKey[Request]("messaging-service")
}
