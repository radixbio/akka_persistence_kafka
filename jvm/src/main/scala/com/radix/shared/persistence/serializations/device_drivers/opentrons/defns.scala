package com.radix.shared.persistence.serializations.device_drivers.opentrons

import java.util.UUID

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.stream.SourceRef
import com.radix.rainbow.Offset
import com.radix.rainbow.URainbow.RainbowModifyCommand
import com.radix.rainbow.URainbow.UTypes.RainbowMetaSerialized
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import squants.{Volume, VolumeFlow}
import squants.space.{Microlitres, Volume}
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.utils.rainbowuservice.RainbowActorProtocol.URainbowCommand
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.Schema

object defns {

  sealed trait PipetteCommand {
    val pipette: Option[List[UUID]]
  }
  final case class AspirateCommand(
    wellPtr: List[UUID],
    pipette: Option[List[UUID]],
    volume: Volume,
    rate: VolumeFlow,
    position: Offset
  ) extends PipetteCommand
  object AspirateCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]], volume: Volume, rate: VolumeFlow): AspirateCommand =
      AspirateCommand(wellPtr, pipette, volume, rate, Offset())

  }

  final case class DispenseCommand(
    wellPtr: List[UUID],
    pipette: Option[List[UUID]],
    volume: Volume,
    rate: VolumeFlow,
    position: Offset
  ) extends PipetteCommand

  object DispenseCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]], volume: Volume, rate: VolumeFlow): DispenseCommand = {
      DispenseCommand(wellPtr, pipette, volume, rate, Offset())
    }
  }

  final case class MoveCommand(
    wellPtr: List[UUID],
    pipette: Option[List[UUID]],
    position: Offset
  ) extends PipetteCommand

  object MoveCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]]): MoveCommand = {
      new MoveCommand(wellPtr, pipette, Offset())
    }
  }

  final case class PickUpTipCommand(wellPtr: List[UUID], pipette: Option[List[UUID]], position: Offset)
      extends PipetteCommand

  object PickUpTipCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]]): PickUpTipCommand = {
      PickUpTipCommand(wellPtr, pipette, Offset())
    }
  }

  final case class DropTipCommand(wellPtr: List[UUID], pipette: Option[List[UUID]], position: Offset)
      extends PipetteCommand
  object DropTipCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]]): DropTipCommand =
      DropTipCommand(wellPtr, pipette, Offset())
  }

  final case class DelayCommand(time: Float, message: Option[String]) extends PipetteCommand {
    override val pipette = None
  }

  final case class BlowoutCommand(
    wellPtr: List[UUID],
    pipette: Option[List[UUID]],
    volume: Volume,
    position: Offset
  ) extends PipetteCommand
  object BlowoutCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]], volume: Volume): BlowoutCommand = {
      BlowoutCommand(wellPtr, pipette, volume, Offset())
    }
  }

  final case class TouchTipCommand(
    wellPtr: List[UUID],
    pipette: Option[List[UUID]],
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  object TouchTipCommand {
    def apply(wellPtr: List[UUID], pipette: Option[List[UUID]], volume: Volume): TouchTipCommand = {
      TouchTipCommand(wellPtr, pipette, volume, Offset())
    }
  }


  class OpentronsAspirateSerializer extends AvroSerializer[AspirateCommand]
  class OpentronsDispenseSerializer extends AvroSerializer[DispenseCommand]
  class OpentronsPickUpTipSerializer extends AvroSerializer[PickUpTipCommand]
  class OpentronsDropTipSerializer extends AvroSerializer[DropTipCommand]
  class OpentronsDelaySerializer extends AvroSerializer[DelayCommand]
  class OpentronsBlowoutSerializer extends AvroSerializer[BlowoutCommand]
  class OpentronsTouchTipSerializer extends AvroSerializer[TouchTipCommand]

  sealed trait OpentronsRequest

  final case class OpentronsOrder(
    steps: SourceRef[PipetteCommand],
    replyToOpt: Option[ActorRef[OpentronsReply]]
  ) extends OpentronsRequest
  case class OpentronsInformationRequest(replyTo: ActorRef[OpentronsInformationResponse]) extends OpentronsRequest

  sealed trait OpentronsEvent
  case class OTErrorOccurred(message: String) extends OpentronsEvent
  case class OTExecutedProtocol() extends OpentronsEvent
  case class OTForwardedRequest() extends OpentronsEvent

  sealed trait OpentronsReply
  case class OpentronsInformationResponse(sn: String, uuid: UUID) extends OpentronsReply
  case class OpentronsCommandStarting(rainbowUpdates: SourceRef[RainbowModifyCommand]) extends OpentronsReply
  case class OpentronsCommandDone() extends OpentronsReply
  case class OpentronsCommandFailed() extends OpentronsReply
  case class OpentronsBusy() extends OpentronsReply

  // Request serializers
  class OpentronsOrderSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OpentronsOrder]
  class OpentronsInformationRequestSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OpentronsInformationRequest]

  // Event serializers
  class OTErrorOccurredAvro extends AvroSerializer[OTErrorOccurred]
  class OTExecutedProtocolAvro extends AvroSerializer[OTExecutedProtocol]
  class OTForwardedRequestAvro extends AvroSerializer[OTForwardedRequest]

  // Response serializers
  class OpentronsInformationResponseSerializer extends AvroSerializer[OpentronsInformationResponse]
  class OpentronsCommandStartingSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OpentronsCommandStarting]
  class OpentronsCommandDoneSerializer extends AvroSerializer[OpentronsCommandDone]
  class OpentronsCommandFailedSerializer extends AvroSerializer[OpentronsCommandFailed]
  class OpentronsBusySerializer extends AvroSerializer[OpentronsBusy]
}
