package com.radix.shared.persistence.serializations.util.prism.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.util.prism.rpc.PrismProtocol

object actorInfo {
  val skey = ServiceKey[PrismProtocol.Request]("prism-uservice")
}
