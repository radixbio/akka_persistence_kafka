package com.radix.shared.persistence.serializations.device_drivers.mock_bioreactor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.serializations.squants.units.LitresPerMinute
import com.radix.shared.persistence.AvroSerializer
import squants.motion.VolumeFlow
import squants.thermal.Temperature
import squants.time.Frequency

object defns {
  sealed trait MockBioreactorEvent

  class MockBioreactorEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[MockBioreactorEvent]

  case class RequestResponseEvent(req: MockBioreactorRequest, res: MockBioreactorResponse) extends MockBioreactorEvent

  class RequestEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[RequestResponseEvent]

  sealed trait MockBioreactorRequest {
    val replyTo: Option[ActorRef[MockBioreactorResponse]]
  }

  class MockBioreactorRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[MockBioreactorRequest]

  sealed trait MockBioreactorResponse

  class MockBioreactorResponseSerializer extends AvroSerializer[MockBioreactorResponse]

  case class SetSetpoints(
    replyTo: Option[ActorRef[MockBioreactorResponse]],
    temperature: Option[Temperature],
    stirrerSpeed: Option[Frequency],
    pH: Option[Double],
    oxygenPercentage: Option[Double]
  ) extends MockBioreactorRequest

  object SetSetpoints {
    def apply(replyTo: Option[ActorRef[MockBioreactorResponse]],
              temperature: Option[Temperature] = None,
              stirrerSpeed: Option[Frequency] = None,
              pH: Option[Double] = None,
              oxygenPercentage: Option[Double] = None): SetSetpoints =
      new SetSetpoints(replyTo, temperature, stirrerSpeed, pH, oxygenPercentage)
  }

  class SetSetpointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetSetpoints]

  // Using a case object here made the serializer cranky
  case class SetpointsSet() extends MockBioreactorResponse

  class SetpointsSetSerializer extends AvroSerializer[SetpointsSet]

  case class GetSetpoints(replyTo: Option[ActorRef[MockBioreactorResponse]]) extends MockBioreactorRequest

  class GetSetpointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetSetpoints]

  case class Setpoints(temperature: Temperature, stirrerSpeed: Frequency, pH: Double, oxygenPercentage: Double)
      extends MockBioreactorResponse

  class SetpointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Setpoints]

  case class GetPumpSetpoints(replyTo: Option[ActorRef[MockBioreactorResponse]]) extends MockBioreactorRequest

  class GetPumpSetpointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetPumpSetpoints]

  case class PumpSetpoints(pumpSetpoints: Map[String, VolumeFlow]) extends MockBioreactorResponse

  class PumpSetpointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[PumpSetpoints]

  case class SetPumpSetpoint(replyTo: Option[ActorRef[MockBioreactorResponse]], pumpName: String, setpoint: VolumeFlow)
      extends MockBioreactorRequest

  class SetPumpSetpointSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetPumpSetpoint]

  // Using a case object here made the serializer cranky
  case class PumpSetpointSet() extends MockBioreactorResponse

  class PumpSetpointSetSerializer extends AvroSerializer[PumpSetpointSet]

  case class PumpNotFound(pumpName: String) extends MockBioreactorResponse

  class PumpNotFoundSerializer extends AvroSerializer[PumpNotFound]
}
