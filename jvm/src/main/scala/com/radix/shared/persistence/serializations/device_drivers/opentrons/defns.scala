package com.radix.shared.persistence.serializations.device_drivers.opentrons

import java.util.UUID

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.stream.SourceRef
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.util.prism.{Container, Offset}
import squants.Volume
import squants.space.Volume
import com.radix.shared.persistence.serializations.utils.prism.Serializers._
import com.radix.shared.persistence.serializations.utils.prism.derivations._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.utils.prism.{PrismMeta, PrismNoMeta}
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import matryoshka.data.Fix
import com.radix.shared.util.prism.implicits._
import com.radix.shared.util.prism.rpc.PrismProtocol.{PrismMetadata, PrismWithMetadata}
import org.apache.avro.Schema

object defns {

  sealed trait PipetteCommand
  final case class AspirateCommand(
    world: PrismNoMeta,
    labwareUID: UUID,
    wellUID: UUID,
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  final case class DispenseCommand(
    world: PrismNoMeta,
    labwareUID: UUID,
    wellUID: UUID,
    volume: Volume,
    position: Offset
  ) extends PipetteCommand

  final case class PickUpTipCommand(world: PrismNoMeta, labwareUID: UUID, wellUID: UUID, position: Offset)
      extends PipetteCommand

  final case class DropTipCommand(world: PrismNoMeta, labwareUID: UUID, wellUID: UUID, position: Offset)
      extends PipetteCommand

  final case class DelayCommand(time: Float, message: Option[String]) extends PipetteCommand

  final case class BlowoutCommand(world: PrismNoMeta, labwareUID: UUID, wellUID: UUID, volume: Volume, position: Offset)
      extends PipetteCommand

  final case class TouchTipCommand(
    world: PrismNoMeta,
    labwareUID: UUID,
    wellUID: UUID,
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
    world: SourceRef[PrismNoMeta],
    opentronsUUID: UUID,
    steps: SourceRef[PipetteCommand],
    replyToOpt: Option[akka.actor.typed.ActorRef[OpentronsReply]]
  ) extends OpentronsRequest

  sealed trait OpentronsReply
  case class OpentronsInformationRequest(replyTo: ActorRef[OpentronsReply]) extends OpentronsRequest
  case class OpentronsInformationResponse(uuid: String) extends OpentronsReply
  case class OpentronsCommandDone() extends OpentronsReply

  class OpentronsInformationRequestSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OpentronsInformationRequest]
  class OpentronsInformationResponseSerializer extends AvroSerializer[OpentronsInformationResponse]
  class OpentronsCommandDoneSerializer extends AvroSerializer[OpentronsCommandDone]
  class OpentronsOrderSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OpentronsOrder]
}
