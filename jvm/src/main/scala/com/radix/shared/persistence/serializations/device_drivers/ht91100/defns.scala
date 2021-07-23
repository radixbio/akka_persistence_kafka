package com.radix.shared.persistence.serializations.device_drivers.ht91100

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.AvroSerializer
import squants.Time
import squants.time.Frequency
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.squants.schemas._

object defns {
  sealed trait Ht91100Event
  class Ht91100EventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Ht91100Event]

  sealed trait Ht91100Request {
    // All reply actors must at least support the MachineOffline message
    val replyTo: Option[ActorRef[MachineOffline.type]]
  }
  class Ht91100RequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Ht91100Request]

  sealed trait Ht91100Response

  class Ht91100ResponseSerializer extends AvroSerializer[Ht91100Response]

  case class RequestResponseEvent(request: Ht91100Request, response: Ht91100Response) extends Ht91100Event

  class RequestResponseEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[RequestResponseEvent]

  /**
   * Starts the shaker using the given amount of time to ramp up to the given velocity.
   * @param acceleration Amount of time it takes shaker to ramp up to target velocity. Must be from 0-10 seconds.
   * @param velocity Velocity at which shaker shakes. Must be from 60-3570 RPM.
   * @param clockwise: Whether shaker should spin clockwise
   */
  case class StartShaker(
    replyTo: Option[ActorRef[CommandResponse]],
    acceleration: Time,
    velocity: Frequency,
    clockwise: Boolean
  ) extends Ht91100Request {
    if (acceleration.toSeconds < 0 || acceleration.toSeconds > 10) {
      throw new IllegalArgumentException("Cannot start shaker with acceleration outside of range 0-10 seconds!")
    }
    if (velocity.toRevolutionsPerMinute < 60 || velocity.toRevolutionsPerMinute > 3570) {
      throw new IllegalArgumentException("Cannot start shaker with velocity outside of range 60-3570 RPM!")
    }
  }

  class StartShakerSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[StartShaker]

  /**
   * Stops the shaker.
   * @param home Whether the shaker should return home after it stops.
   */
  case class StopShaker(replyTo: Option[ActorRef[CommandResponse]], home: Boolean) extends Ht91100Request

  class StopShakerSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[StopShaker]

  /**
   * Tells the shaker to return to home.
   */
  case class FindHome(replyTo: Option[ActorRef[CommandResponse]]) extends Ht91100Request

  class FindHomeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[FindHome]

  sealed trait CommandResponse extends Ht91100Response

  class CommandResponseSerializer extends AvroSerializer[CommandResponse]

  case object CommandReceived extends CommandResponse

  case class GetStatus(replyTo: Option[ActorRef[StatusResponse]]) extends Ht91100Request

  class GetStatusSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetStatus]

  sealed trait StatusResponse extends Ht91100Response

  class StatusResponseSerializer extends AvroSerializer[StatusResponse]

  sealed abstract class ShakerState(val asString: String)

  object ShakerState {
    case object Stopped extends ShakerState("STOP")
    case object Running extends ShakerState("RUN")
    case object RampingUp extends ShakerState("RAMP+")
    case object RampingDown extends ShakerState("RAMP-")

    def fromString(string: String): ShakerState =
      string match {
        case Stopped.asString     => Stopped
        case Running.asString     => Running
        case RampingUp.asString   => RampingUp
        case RampingDown.asString => RampingDown
        case _                    => throw new IllegalArgumentException(s"String $string does not represent a ShakerState!")
      }

  }

  case class Status(setAcceleration: Time, setVelocity: Frequency, actualVelocity: Frequency, state: ShakerState)
      extends StatusResponse

  class StatusSerializer extends AvroSerializer[Status]

  case class GetMetadata(replyTo: Option[ActorRef[MetadataResponse]]) extends Ht91100Request

  class GetMetadataSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetMetadata]

  sealed trait MetadataResponse extends Ht91100Response

  class MetadataResponseSerializer extends AvroSerializer[MetadataResponse]

  case class Metadata(softwareVersion: String, serialNumber: String, modelNumber: String) extends MetadataResponse

  class MetadataSerializer extends AvroSerializer[Metadata]

  case object MachineOffline extends Ht91100Response with CommandResponse with StatusResponse with MetadataResponse

}
