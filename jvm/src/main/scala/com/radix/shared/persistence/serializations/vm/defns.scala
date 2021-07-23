package com.radix.shared.persistence.serializations.vm

import java.util.UUID

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.rainbow.LabMeta
import com.radix.shared.persistence.AvroSerializer
import squants.space.Volume
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.ActorRefSerializer._
import com.sksamuel.avro4s.{Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.Schema
import shapeless.the

object defns {

  // VM Type Definitions
  // Id type used as rainbow key
  type VMId = UUID
  // type of metadata
  type Metadata = Map[String, String]
  // container type used in rainbow
  type VMData = LabMeta[Metadata]

  type Ptr = List[VMId]
  type PtrMeta = Map[String, String]

  // VM bytecode definitions used to construct messages for the VM
  sealed trait Bytecode
  //basics
  case class True() extends Bytecode
  case class False() extends Bytecode
  case class And(lhs: Bytecode, rhs: Bytecode) extends Bytecode
  case class Or(lhs: Bytecode, rhs: Bytecode) extends Bytecode
  case class VInt(int: Int) extends Bytecode

  //structure
  case class BytecodeBlock(stmts: List[Bytecode]) extends Bytecode

  //pipetttor
  case class Asp(volume: Volume) extends Bytecode
  case class Disp(volume: Volume) extends Bytecode
  case class Mov(where: Ptr) extends Bytecode

  //control flow
  case class ITE(condition: Bytecode, tru: Bytecode, fals: Bytecode) extends Bytecode

  //sensor
  case class SensorReading(whichSensor: String) extends Bytecode

  //constraints
  //  case class ConstraintStart[A](which: String, expr: ZIO[Any, Nothing, Boolean]) extends Bytecode[A]
  case class ConstraintStart(which: String) extends Bytecode

  case class ConstraintEnd(which: String) extends Bytecode

  implicit def schemaForBytecode = the[SchemaFor[Bytecode]]

  implicit def encoderForBytecode = the[Encoder[Bytecode]]
  implicit def decodedForBytecode = the[Decoder[Bytecode]]

  trait VMRequest

  case class SendProgram(program: Bytecode, returnPID: Option[ActorRef[Int]], signalDone: Option[ActorRef[Int]])
      extends VMRequest

  case class KillProcess(PID: Int) extends VMRequest

  class BytecodeSerializer extends AvroSerializer[Bytecode]
  class SendProgramSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SendProgram]
  class KillProcessSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[KillProcess]
}
