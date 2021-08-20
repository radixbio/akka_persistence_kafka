package com.radix.shared.persistence.serializations.device_drivers.gali_motion_controller

import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.serializations.device_drivers.gali_motion_controller.defns.OctetGali._
import com.radix.shared.persistence.serializations.device_drivers.common.requests.DriverRequest

object actorInfo {
  val octetServiceKey: ServiceKey[DriverRequest[OctetMotionCtrlrRequest]] = ServiceKey("gali-motion-controller-octet")
}
