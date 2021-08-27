package com.radix.shared.persistence.serializations.bioutil

object defns {
  sealed trait EaselRequest

  case class RetrievedProcessID(id: Int) extends EaselRequest
  case class ProcessTerminated(pid: Int) extends EaselRequest


}
