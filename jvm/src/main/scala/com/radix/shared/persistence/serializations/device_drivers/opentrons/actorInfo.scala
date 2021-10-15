package com.radix.shared.persistence.serializations.device_drivers.opentrons

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.archetypes.transactions.{CapTxn, Txn, TxnMsg}
//import com.radix.shared.persistence.serializations.device_drivers.archetypes.transactions.TransactionReq
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest
import com.radix.shared.persistence.serializations.device_drivers.opentrons.defns.{OpentronsRequest, PipetteCommand}

object actorInfo {
  val skey = ServiceKey[DriverRequest[OpentronsRequest]]("opentrons-actor")

  val archetypeSkey: ServiceKey[TxnMsg] = ServiceKey[TxnMsg]("opentrons-archetype")

}
