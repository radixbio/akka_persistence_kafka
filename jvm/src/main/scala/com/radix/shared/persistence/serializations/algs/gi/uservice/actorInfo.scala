package com.radix.shared.persistence.serializations.algs.gi.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.algs.gi.uservice.protocol.IsomorphismRequest

object actorInfo {
  val skey = ServiceKey[IsomorphismRequest]("gi")
}
