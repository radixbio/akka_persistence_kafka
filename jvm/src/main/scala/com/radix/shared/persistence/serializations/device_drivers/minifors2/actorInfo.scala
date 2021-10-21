package com.radix.shared.persistence.serializations.device_drivers.minifors2

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.archetypes.bioreactor.BioreactorCommand
import com.radix.shared.persistence.serializations.device_drivers.archetypes.transactions.TxnMsg
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.minifors2.defns.Minifors2Request

object actorInfo {
  val skey: ServiceKey[DriverRequest[Minifors2Request]] = ServiceKey[DriverRequest[Minifors2Request]]("minifors2")
  val archetypeSkey: ServiceKey[TxnMsg] = ServiceKey[TxnMsg]("minifors2-transactor")
}
