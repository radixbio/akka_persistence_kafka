package com.radix.shared.persistence.serializations.device_drivers.mock_bioreactor

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.archetypes.transactions.TxnMsg
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.mock_bioreactor.defns.MockBioreactorRequest

object actorInfo {
  val skey: ServiceKey[DriverRequest[MockBioreactorRequest]] =
    ServiceKey[DriverRequest[MockBioreactorRequest]]("mock-bioreactor")

  val archetypeSkey: ServiceKey[TxnMsg] = ServiceKey[TxnMsg]("mock-bioreactor-archetype")
}
