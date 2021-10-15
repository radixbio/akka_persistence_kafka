package com.radix.shared.persistence.serializations.device_drivers.archetypes

import java.util.UUID

import akka.stream.SourceRef
import squants.motion.VolumeFlow
import squants.space.Volume
import com.radix.rainbow.URainbow.RainbowModifyCommand
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.vm.defns.Ptr
import com.radix.shared.persistence.serializations.squants.schemas._

object liquid_handler {
  sealed trait PipettingCommand
  sealed trait TipCyclingCommand

  case class Asp(vol: Volume, rate: VolumeFlow, pipettePtr: Ptr) extends PipettingCommand
  case class Disp(vol: Volume, rate: VolumeFlow, pipettePtr: Ptr) extends PipettingCommand
  case class Mov(where: List[UUID], pipettePtr: Ptr) extends PipettingCommand

  case class PickUpTip(pipettePtr: Ptr) extends TipCyclingCommand
  case class DropTip(pipettePtr: Ptr) extends TipCyclingCommand

  sealed trait LiquidHandlingCommand
  case class Pipette(cmd: PipettingCommand) extends LiquidHandlingCommand
  case class TipCycle(cmd: TipCyclingCommand) extends LiquidHandlingCommand

  implicit def liftPipette(p: PipettingCommand): LiquidHandlingCommand = Pipette(p)
  implicit def liftTipCycle(tc: TipCyclingCommand): LiquidHandlingCommand = TipCycle(tc)

  class AspAvro extends AvroSerializer[Asp]
  class DispAvro extends AvroSerializer[Disp]
  class MovAvro extends AvroSerializer[Mov]
  class PickUpTipAvro extends AvroSerializer[PickUpTip]
  class DropTipAvro extends AvroSerializer[DropTip]

  class PipetteAvro extends AvroSerializer[Pipette]
  class TipCycleAvro extends AvroSerializer[TipCycle]

}
