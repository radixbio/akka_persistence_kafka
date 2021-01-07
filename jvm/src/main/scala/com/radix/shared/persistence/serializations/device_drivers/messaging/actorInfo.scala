package com.radix.shared.persistence.serializations.device_drivers.messaging

import akka.actor.typed.receptionist.ServiceKey
import defns.UserMessagingRequest

object actorInfo {
  val skey = ServiceKey[UserMessagingRequest]("messaging-service")
}
