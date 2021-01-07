package com.radix.shared.persistence.serializations.device_drivers.tfexactive

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.tfexactive.defns.TFExactiveRequest

import scala.reflect.ClassTag

object actorInfo {
  val skey = ServiceKey[TFExactiveRequest]("tf-exactive-actor")
}
