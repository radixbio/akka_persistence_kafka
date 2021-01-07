package com.radix.shared.persistence.serializations.device_drivers.multitrons

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.multitrons.defns.MultitronsCommand

import scala.reflect.ClassTag

object actorInfo {
  val skey = ServiceKey[MultitronsCommand]("multitrons-driver")
}
