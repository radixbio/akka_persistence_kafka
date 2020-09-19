package com.radix.shared.persistence.serializations.device_drivers.tfexactive

import akka.actor.typed.ActorRef

object defns {

  sealed trait LNLevel
  final case class LNLevelLow() extends LNLevel
  final case class LNLevelNormal() extends LNLevel
  final case class LNLevelRequest(replyTo: ActorRef[LNLevel])
}
