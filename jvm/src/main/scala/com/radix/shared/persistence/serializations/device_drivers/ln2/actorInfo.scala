package com.radix.shared.persistence.serializations.device_drivers.ln2

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.ln2.defns.LNLevelRequest

import scala.reflect.ClassTag

object actorInfo {
  val skey = ServiceKey[LNLevelRequest]("ln2-actor")
}
