package com.radix.shared.persistence.serializations.device_drivers.opentrons

import java.util.UUID

import com.radix.shared.persistence.AvroSerializer
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
  final case class PickTipCommand(world: PrismNoMeta, labwareUID: UUID, wellUID: UUID, position: Offset)
      extends PipetteCommand
  final case class DropTipCommand(world: PrismNoMeta, labwareUID: UUID, wellUID: UUID, volume: Volume, position: Offset)
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
  sealed trait OpentronsRequest
  final case class OpentronsOrder(world: PrismNoMeta, steps: PipetteProcedure) extends OpentronsRequest

  class OpentronsOrderSerializer extends AvroSerializer[OpentronsOrder]
}
