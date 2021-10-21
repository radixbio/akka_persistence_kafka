package com.radix.shared.persistence.serializations.device_drivers.archetypes

import akka.actor.ExtendedActorSystem
import com.radix.shared.persistence.AvroSerializer
import squants.Temperature
import squants.motion.VolumeFlow
import squants.time.Frequency
import com.radix.shared.persistence.serializations.utils.akka.promise.PromiseRef
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.squants.schemas._

object bioreactor {

//  case object PumpFlowRateF extends BioreactorFeature[VolumeFlow] TODO: Determine necessity

  // to allow correct serialization
  case class Percentage(percent: Double)
  case class PH(level: Double)

  class PercentageAvro extends AvroSerializer[Percentage]
  class PHAvro extends AvroSerializer[PH]

  sealed trait BioreactorCommand

  sealed trait BioreactorSetCmd extends BioreactorCommand
  case class SetTemperature(target: Temperature) extends BioreactorSetCmd
  case class SetStirrerSpeed(target: Frequency) extends BioreactorSetCmd
  case class SetPH(target: PH) extends BioreactorSetCmd
  case class SetO2Level(targetPercentage: Percentage) extends BioreactorSetCmd
  case class SetPumpFlowRate(pumpID: String, target: VolumeFlow) extends BioreactorSetCmd

  class SetTemperatureAvro extends AvroSerializer[SetTemperature]
  class SetStirrerSpeedAvro extends AvroSerializer[SetStirrerSpeed]
  class SetPHAvro extends AvroSerializer[SetPH]
  class SetO2LevelAvro extends AvroSerializer[SetO2Level]
  class SetPumpFlowRateAvro extends AvroSerializer[SetPumpFlowRate]

  sealed trait BioreactorGetCmd extends BioreactorCommand {
    val resolve: PromiseRef
  }
  case class GetTemperature(resolve: PromiseRef) extends BioreactorGetCmd
  case class GetStirrerSpeed(resolve: PromiseRef) extends BioreactorGetCmd
  case class GetPH(resolve: PromiseRef) extends BioreactorGetCmd
  case class GetO2Level(resolve: PromiseRef) extends BioreactorGetCmd
  case class GetPumpFlowRate(pumpID: String, resolve: PromiseRef) extends BioreactorGetCmd

  class GetTemperatureAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetTemperature]
  class GetStirrerSpeedAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetStirrerSpeed]
  class GetPHAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetPH]
  class GetO2LevelAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetO2Level]
  class GetPumpFlowRateAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetPumpFlowRate]

  sealed trait BioreactorResponse
  case class OperationSucceeded() extends BioreactorResponse
  case class OperationFailed(cause: String) extends BioreactorResponse
}
