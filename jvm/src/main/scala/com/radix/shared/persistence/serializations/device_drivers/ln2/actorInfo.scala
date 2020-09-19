package com.radix.shared.persistence.serializations.device_drivers.ln2

import akka.actor.typed.receptionist.ServiceKey

import scala.reflect.ClassTag

object actorInfo {
  def skey[T](implicit classTag: ClassTag[T]) = ServiceKey[T]("ln2-actor")
}
