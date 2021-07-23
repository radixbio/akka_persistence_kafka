package com.radix.shared.persistence.serializations.device_drivers.ln2

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer

object defns {

  sealed trait LNLevel
  final case class LNLevelLow() extends LNLevel
  final case class LNLevelNormal() extends LNLevel

  final case class LNLevelRequest(replyTo: Option[ActorRef[LNLevel]]) {
    def reply(msg: LNLevel): Unit = replyTo match {
      case None        => // Ignore
      case Some(value) => value ! msg
    }
  }

  final case class LNLevelCheckEvent(value: LNLevel, requester: Option[ActorRef[LNLevel]])

  class LNLevelCheckEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[LNLevelCheckEvent]

  class LNLevelLowSerializer extends AvroSerializer[LNLevelLow]
  class LNLevelNormalSerializer extends AvroSerializer[LNLevelNormal]

  class LNLevelRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[LNLevelRequest]
}
