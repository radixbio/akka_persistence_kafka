package com.radix.shared.persistence.serializations.device_drivers.tfexactive

import akka.actor.typed.ActorRef

object defns {
  sealed trait TFExactiveRequest

  sealed trait TFExactiveStdRequest extends TFExactiveRequest
  sealed trait TFExactiveSeqRequest extends TFExactiveRequest

  case class TFERunMethod(methodPath: String, outputPath: String) extends TFExactiveStdRequest

  case class TFEExecuteSequence(seqPath: String) extends TFExactiveSeqRequest

  sealed trait TFExactiveCommonRequest extends TFExactiveStdRequest with TFExactiveSeqRequest
  case class TFEGetSummary(replyTo: Option[ActorRef[TFESummaryResponse]]) extends TFExactiveCommonRequest

  sealed trait TFExactiveEvent

  sealed trait TFExactiveSeqEvent extends TFExactiveEvent
  case class TFEStartedSequence(fnm: String) extends TFExactiveSeqEvent

  sealed trait TFExactiveStdEvent extends TFExactiveEvent
  case class RanMethod(methodName: String) extends TFExactiveStdEvent

  sealed trait TFExactiveCommonEvent extends TFExactiveSeqEvent with TFExactiveStdEvent
  case class TFEExecutionError(cause: String) extends TFExactiveCommonEvent

  sealed trait TFExactiveResponse

  sealed trait TFExactiveStdResponse extends TFExactiveResponse
  sealed trait TFExactiveSeqResponse extends TFExactiveResponse

  sealed trait TFExactiveCommonResponse extends TFExactiveStdResponse with TFExactiveSeqResponse
  case class TFESummaryResponse(data: Map[String, String]) extends TFExactiveCommonResponse

}
