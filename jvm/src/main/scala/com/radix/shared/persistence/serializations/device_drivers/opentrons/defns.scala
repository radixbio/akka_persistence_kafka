package com.radix.shared.persistence.serializations.device_drivers.opentrons

import java.util.UUID

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.stream.SourceRef
import com.radix.rainbow.Offset
import com.radix.rainbow.URainbow.UTypes.RainbowMetaSerialized
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import squants.Volume
import squants.space.Volume
import com.radix.shared.persistence.serializations.squants.schemas._
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.Schema

object defns {

  sealed trait PipetteCommand
  final case class AspirateCommand(
    world: RainbowMetaSerialized,
    labwareUID: String,
    wellUID: String,
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  final case class DispenseCommand(
    world: RainbowMetaSerialized,
    labwareUID: String,
    wellUID: String,
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  final case class PickUpTipCommand(world: RainbowMetaSerialized, labwareUID: String, wellUID: String, position: Offset)
      extends PipetteCommand

  final case class DropTipCommand(world: RainbowMetaSerialized, labwareUID: String, wellUID: String, position: Offset)
      extends PipetteCommand

  final case class DelayCommand(time: Float, message: Option[String]) extends PipetteCommand

  final case class BlowoutCommand(world: RainbowMetaSerialized, labwareUID: String, wellUID: String, volume: Volume, position: Offset)
      extends PipetteCommand

  final case class TouchTipCommand(
    world: RainbowMetaSerialized,
    labwareUID: String,
    wellUID: String,
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  final case class PipetteProcedure(steps: List[PipetteCommand])

  class OpentronsAspirateSerializer extends AvroSerializer[AspirateCommand]
  class OpentronsDispenseSerializer extends AvroSerializer[DispenseCommand]
  class OpentronsPickUpTipSerializer extends AvroSerializer[PickUpTipCommand]
  class OpentronsDropTipSerializer extends AvroSerializer[DropTipCommand]
  class OpentronsDelaySerializer extends AvroSerializer[DelayCommand]
  class OpentronsBlowoutSerializer extends AvroSerializer[BlowoutCommand]
  class OpentronsTouchTipSerializer extends AvroSerializer[TouchTipCommand]

  sealed trait OpentronsRequest

  final case class OpentronsOrder(
    world: SourceRef[RainbowMetaSerialized],
    opentronsUUID: String,
    steps: SourceRef[PipetteCommand],
    replyToOpt: Option[akka.actor.typed.ActorRef[OpentronsReply]]
  ) extends OpentronsRequest

  sealed trait OpentronsEvent
  sealed trait OpentronsHWEvent
  case class OTErrorOccurred(message:String) extends OpentronsEvent with OpentronsHWEvent
  case class OTExecutedProtocol() extends OpentronsEvent with OpentronsHWEvent
  case class OTForwardedRequest() extends OpentronsEvent

  case class OTHWStartingProtocol(replyTo: Option[akka.actor.typed.ActorRef[OpentronsReply]]) extends OpentronsHWEvent

  class OTErrorOccurredAvro extends AvroSerializer[OTErrorOccurred]
  class OTExecutedProtocolAvro extends AvroSerializer[OTExecutedProtocol]
  class OTForwardedRequestAvro extends AvroSerializer[OTForwardedRequest]

  class OTHWStartingProtocolAvro(implicit eas: ExtendedActorSystem) extends AvroSerializer[OTHWStartingProtocol]

  sealed trait OpentronsReply
  case class OpentronsInformationRequest(replyTo: ActorRef[OpentronsReply]) extends OpentronsRequest
  case class OpentronsInformationResponse(uuid: String) extends OpentronsReply
  case class OpentronsCommandDone() extends OpentronsReply
  case class OpentronsCommandFailed() extends OpentronsReply


  class OpentronsInformationResponseSerializer extends AvroSerializer[OpentronsInformationResponse]
  class OpentronsCommandDoneSerializer extends AvroSerializer[OpentronsCommandDone]
  class OpentronsCommandFailedSerializer extends AvroSerializer[OpentronsCommandFailed]

  class OpentronsOrderSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OpentronsOrder]
  class OpentronsInformationRequestSerializer(implicit eas: ExtendedActorSystem)
    extends AvroSerializer[OpentronsInformationRequest]
}
