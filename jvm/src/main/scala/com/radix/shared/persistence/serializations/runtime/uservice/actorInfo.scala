package com.radix.shared.persistence.serializations.runtime.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.runtime.Protocol.Request

object actorInfo {
  final val skey = ServiceKey[Request]("radixServiceDiscovery")

}
