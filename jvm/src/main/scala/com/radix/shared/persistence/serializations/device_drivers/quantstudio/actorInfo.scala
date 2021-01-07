package com.radix.shared.persistence.serializations.device_drivers.quantstudio

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.quantstudio.defns.QuantstudioRequest

import scala.reflect.ClassTag

object actorInfo {
  val skey = ServiceKey[QuantstudioRequest]("quantstudio-actor")
}
