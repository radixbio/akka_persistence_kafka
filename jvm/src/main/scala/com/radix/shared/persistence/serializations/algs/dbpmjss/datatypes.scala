package com.radix.shared.persistence.serializations.algs.dbpmjss

import com.radix.shared.persistence.AvroSerializer
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}

import scala.collection.immutable.{List, Map, SortedMap}
import shapeless.the

object datatypes {
  type DurationL = Long
  type TimeL = Long
  type NoBuffer = SortedMap[TimeL, (DurationL, Operation)] //start, blocking, operation
  final implicit object schemaForNoBuffer extends SchemaFor[NoBuffer] {
      override def schema: Schema =
      the[SchemaFor[List[(TimeL, (DurationL, Operation))]]].schema
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }
  final implicit object encoderForNoBuffer extends  Encoder[NoBuffer]  {
    override def encode(t: NoBuffer): AnyRef =
      the[Encoder[List[(TimeL, (DurationL, Operation))]]].encode(t.toList)
    override def schemaFor: SchemaFor[NoBuffer] = schemaForNoBuffer

  }

  final implicit object decoderForNoBuffer extends Decoder[NoBuffer] {
      override def decode(value: Any): NoBuffer =
      SortedMap(the[Decoder[List[(TimeL, (DurationL, Operation))]]].decode(value): _*)
    override def schemaFor: SchemaFor[NoBuffer] = schemaForNoBuffer

  }

  type WithBuffer = (NoBuffer, List[NoBuffer]) // the main machine, and the set of buffers
  type WithBufferList = List[NoBuffer]
  type MultipleMachines = Either[List[NoBuffer], List[WithBufferList]]
  type Indexed = Map[MachineIndex, Either[NoBuffer, WithBuffer]]

  final case class OperationIndex(id: Int) extends AnyVal // Dependency order
  final object OperationIndex {
    implicit val schema = SchemaFor[OperationIndex]

  }

  final case class JobIndex(id: Int) extends AnyVal
  final object JobIndex {
    implicit val schema = SchemaFor[JobIndex]
  }
  final case class Job(name: String, ops: List[(OperationIndex, Operation)])
  sealed trait JobReq
  final case object JobRequires extends JobReq
  final case object JobDoesNotRequire extends JobReq

  final case class Jobs(jobs: List[(JobIndex, Job)])

  /**
   * The buffertype denotes the ability of a machine to buffer it's output after it's been completed
   */
  sealed trait BufferType

  /**
   * No-Wait implies that the next task must start directly after the current task, no buffering or blocking permitted
   */
  final case object NoWait extends BufferType

  /**
   * No-Buffer implies that the next task may block the previous task, but not be placed in a buffer to avoid blocking
   */
  final case object NoBuffer extends BufferType

  /**
   * Limited-buffer implies a finite number of buffers that the machine contains in order to continue making forward progress
   * while child tasks have not yet started being processed by moving the job to a buffer zone
   */
  final case class LimitedBuffer(numbuffers: Int) extends BufferType

  /**
   * An infinite number of buffers
   */
  final case object InfiniteBuffer extends BufferType

  final case class Machine(name: String, bufferType: BufferType, replicas: Int = 1)
  final case class MachineIndex(id: Int) extends AnyVal
  final object MachineIndex {
    implicit val schema = SchemaFor[MachineIndex]
  }
  final implicit def MidxMapAvroSchema[T: SchemaFor: Encoder: Decoder]
    : SchemaFor[Map[MachineIndex, T]]  = {
    new SchemaFor[Map[MachineIndex, T]]  {
      override def schema: Schema =
        the[SchemaFor[List[(MachineIndex, T)]]].schema
      override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
    }
  }
  final implicit def MidxMapAvroEncode[T: SchemaFor: Encoder: Decoder]
  : Encoder[Map[MachineIndex, T]]= {
    new Encoder[Map[MachineIndex, T]]{

      override def encode(t: Map[MachineIndex, T]): AnyRef =
        the[Encoder[List[(MachineIndex, T)]]].encode(t.toList)
      override def schemaFor: SchemaFor[Map[MachineIndex, T]] = MidxMapAvroSchema[T]

    }

  }
  final implicit def MidxMapAvroDecode[T: SchemaFor: Encoder: Decoder]
  : Decoder[Map[MachineIndex, T]] = {
    new Decoder[Map[MachineIndex, T]] {
      override def decode(value: Any): Map[MachineIndex, T] =
        the[Decoder[List[(MachineIndex, T)]]].decode(value).toMap
      override def schemaFor: SchemaFor[Map[MachineIndex, T]] = MidxMapAvroSchema[T]

    }
  }

  final case class Machines(machines: Map[MachineIndex, Machine])
  final case class Operation(
    name: String,
    makespan: Long,
    machine: Machine,
    prev: List[OperationIndex],
    up: List[OperationIndex],
    thisIndex: OperationIndex
  )

  final case class Schedule(schedule: Map[MachineIndex, MultipleMachines], M: Machines, J: Jobs)

  class DBPMJSSSchedule extends AvroSerializer[Schedule]
  class DBPMJSSMachines extends AvroSerializer[Machines]
  class DBPMJSSJobs extends AvroSerializer[Jobs]

}
