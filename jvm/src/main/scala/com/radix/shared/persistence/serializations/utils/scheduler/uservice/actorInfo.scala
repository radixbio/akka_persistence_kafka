package com.radix.shared.persistence.serializations.utils.scheduler.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.utils.scheduleruservice.SchedulerActorProtocol.USchedulerCommand

object actorInfo {
  val skey = ServiceKey[USchedulerCommand]("scheduler-uservice")
}
