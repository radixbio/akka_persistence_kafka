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
  final implicit object schemaForNoBuffer extends SchemaFor[NoBuffer] with Encoder[NoBuffer] with Decoder[NoBuffer] {
    override def encode(t: NoBuffer, schema: Schema, fieldMapper: FieldMapper): AnyRef =
      the[Encoder[List[(TimeL, (DurationL, Operation))]]].encode(t.toList, schema, fieldMapper)
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): NoBuffer =
      SortedMap(the[Decoder[List[(TimeL, (DurationL, Operation))]]].decode(value, schema, fieldMapper): _*)
    override def schema(fieldMapper: FieldMapper): Schema =
      the[SchemaFor[List[(TimeL, (DurationL, Operation))]]].schema(fieldMapper)
  }

  type WithBuffer = (NoBuffer, List[NoBuffer]) // the main machine, and the set of buffers
  type WithBufferList = List[NoBuffer]
  type MultipleMachines = Either[List[NoBuffer], List[WithBufferList]]
  type Indexed = Map[MachineIndex, Either[NoBuffer, WithBuffer]]

  final case class OperationIndex(id: Int) extends AnyVal // Dependency order
  final object OperationIndex {
    implicit object schema extends SchemaFor[OperationIndex] with Encoder[OperationIndex] with Decoder[OperationIndex] {
      override def schema(fieldMapper: FieldMapper): Schema = the[SchemaFor[Int]].schema(fieldMapper)
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): OperationIndex =
        OperationIndex(the[Decoder[Int]].decode(value, schema, fieldMapper))
      override def encode(t: OperationIndex, schema: Schema, fieldMapper: FieldMapper): AnyRef =
        the[Encoder[Int]].encode(t.id, schema, fieldMapper)
    }

  }

  final case class JobIndex(id: Int) extends AnyVal
  final object JobIndex {
    implicit object schema extends SchemaFor[JobIndex] with Encoder[JobIndex] with Decoder[JobIndex] {
      override def schema(fieldMapper: FieldMapper): Schema = the[SchemaFor[Int]].schema(fieldMapper)
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): JobIndex =
        JobIndex(the[Decoder[Int]].decode(value, schema, fieldMapper))
      override def encode(t: JobIndex, schema: Schema, fieldMapper: FieldMapper): AnyRef =
        the[Encoder[Int]].encode(t.id, schema, fieldMapper)
    }
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
    implicit object schema extends SchemaFor[MachineIndex] with Encoder[MachineIndex] with Decoder[MachineIndex] {
      override def schema(fieldMapper: FieldMapper): Schema = the[SchemaFor[Int]].schema(fieldMapper)
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): MachineIndex =
        MachineIndex(the[Decoder[Int]].decode(value, schema, fieldMapper))
      override def encode(t: MachineIndex, schema: Schema, fieldMapper: FieldMapper): AnyRef =
        the[Encoder[Int]].encode(t.id, schema, fieldMapper)
    }
  }
  final implicit def MidxMapAvro[T: SchemaFor: Encoder: Decoder]
    : SchemaFor[Map[MachineIndex, T]] with Encoder[Map[MachineIndex, T]] with Decoder[Map[MachineIndex, T]] = {
    new SchemaFor[Map[MachineIndex, T]] with Encoder[Map[MachineIndex, T]] with Decoder[Map[MachineIndex, T]] {
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Map[MachineIndex, T] =
        the[Decoder[List[(MachineIndex, T)]]].decode(value, schema, fieldMapper).toMap

      override def encode(t: Map[MachineIndex, T], schema: Schema, fieldMapper: FieldMapper): AnyRef =
        the[Encoder[List[(MachineIndex, T)]]].encode(t.toList, schema, fieldMapper)

      override def schema(fieldMapper: FieldMapper): Schema =
        the[SchemaFor[List[(MachineIndex, T)]]].schema(fieldMapper)
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
