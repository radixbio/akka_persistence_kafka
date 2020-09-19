package com.radix.shared.persistence.serializations.device_drivers.quantstudio

import akka.actor.typed.receptionist.ServiceKey

import scala.reflect.ClassTag

object actorInfo {
  def skey[T](implicit classTag: ClassTag[T]) = ServiceKey[T]("quantstudio-actor")
}
