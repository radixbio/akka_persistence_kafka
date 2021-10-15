package com.radix.shared.persistence.serializations.device_drivers.archetypes

import squants.Temperature
import squants.motion.VolumeFlow
import squants.time.Frequency

object bioreactor {
  sealed trait BioreactorCommand

  case class SetTemperature(target: Temperature) extends BioreactorCommand
  case class SetStirrerSpeed(target: Frequency) extends BioreactorCommand
  case class SetPH(target: Double) extends BioreactorCommand
  case class SetO2Level(targetPercentage: Double) extends BioreactorCommand
  case class SetPumpFlowRate(pumpID: String, target: VolumeFlow) extends BioreactorCommand

  sealed trait BioreactorResponse
  case class OperationSucceeded() extends BioreactorResponse
  case class OperationFailed(cause: String) extends BioreactorResponse
}
