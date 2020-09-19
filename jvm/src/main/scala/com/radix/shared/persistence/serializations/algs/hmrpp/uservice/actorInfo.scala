package com.radix.shared.persistence.serializations.algs.hmrpp.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.algs.hmrpp.uservice.protocol.HMRPPRequest

object actorInfo {
  val skey = ServiceKey[HMRPPRequest]("hmrpp")
}
