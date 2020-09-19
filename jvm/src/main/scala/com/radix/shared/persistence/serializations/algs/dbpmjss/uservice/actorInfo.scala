package com.radix.shared.persistence.serializations.algs.dbpmjss.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.algs.dbpmjss.uservice.protocol.Solve

object actorInfo {
  val skey = ServiceKey[Solve]("dbpmjss")
}
