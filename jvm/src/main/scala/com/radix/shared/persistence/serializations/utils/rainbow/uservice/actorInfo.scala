package com.radix.shared.persistence.serializations.utils.rainbow.uservice

import akka.actor.typed.receptionist.ServiceKey
import com.radix.utils.rainbowuservice.RainbowActorProtocol

object actorInfo {
  val skey = ServiceKey[RainbowActorProtocol.URainbowCommand]("rainbow-uservice")
}
