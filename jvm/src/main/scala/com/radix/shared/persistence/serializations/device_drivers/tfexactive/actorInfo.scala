package com.radix.shared.persistence.serializations.device_drivers.tfexactive

import akka.actor.typed.receptionist.ServiceKey

import com.radix.shared.persistence.serializations.device_drivers.tfexactive.defns.{TFExactiveRequest, TFExactiveSeqRequest, TFExactiveStdRequest}
import scala.reflect.ClassTag

import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest

object actorInfo {
  val stdKey = ServiceKey[DriverRequest[TFExactiveStdRequest]]("tf-exactive-actor")
  val seqKey = ServiceKey[DriverRequest[TFExactiveSeqRequest]]("tf-exactive-sequence-actor")
}
