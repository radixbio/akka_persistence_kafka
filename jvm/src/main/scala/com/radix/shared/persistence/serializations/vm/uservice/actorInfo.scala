package com.radix.shared.persistence.serializations.vm.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.vm.defns.VMRequest

object actorInfo {
  val vmServiceID = "VM-service"
  val skey = ServiceKey[VMRequest](vmServiceID)
}
