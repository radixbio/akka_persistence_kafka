package com.radix.shared.persistence.serializations.utils.akka

import akka.actor.typed.ActorRef

//TODO docstring why tf this exists
object promise {
  sealed trait PromiseReq
  case class Fulfill(value: Any) extends PromiseReq
  case class Cancel() extends PromiseReq
  case class Query(replyTo: ActorRef[Either[String, Any]]) extends PromiseReq

  type PromiseRef = ActorRef[PromiseReq]
}
