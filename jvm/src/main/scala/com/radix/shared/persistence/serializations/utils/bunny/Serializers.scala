package com.radix.shared.persistence.serializations.utils.bunny

import java.util.UUID

import com.radix.shared.defs.pipettingrobot.OpentronsPlateModel.{OriginOffset, OriginOffsetUnits, PipetteTipData, PlatePropertiesUnits, WellID, WellProperties, WellPropertiesUnits, WellsU}
import com.radix.shared.util.prism._
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import matryoshka.data.cofree._
import io.circe.parser.parse
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import scalaz.{Cofree, Functor}
import ujson.{read, Js}

import scala.collection.JavaConverters._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.util.prism._
import com.radix.shared.util.prism.implicits._
import squants.space.{Millilitres, Volume}

import scala.collection.JavaConverters._
import java.util

import akka.actor.ExtendedActorSystem

import scala.concurrent.duration._
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._

object Protocol {

  sealed trait InputBunny
  case class InputBunnyRetool(retoolInput: RetoolInput, replyTo: ActorRef[ScheduleGenResponse]) extends InputBunny
  case class InputBunnyUi(retoolInput: UiInput, replyTo: ActorRef[ScheduleGenResponse]) extends InputBunny

  case class UiInput(
    human: String, //This is a hack. Any resource should have shifts
    maxshifttime: String, //Same hack as above. Bunny has to change before this, each resources should be personalizable
    toPerturbate: String, //Same hack as above. Should come from the machines. if they have shifts, perturbate the shifts.
    shiftresolution: String, //Other bad hack, since we sample schedules randomly instead of using hill climbing or some heuristic
    machinekupperbound: String, //technical debt from the previous ui
    machineklowerbound: String, //technical debt from the previous ui
    ndays: String, //Quite possibly the worst of all the hacks the scheduler uses
    scenario: String, //name of scenario
    tasks: List[UiTask],
    machines: List[UiMachine],
    topk: String //everything is a string because it comes from a form
  );

  case class UiTask(
    id: String,
    name: String,
    duration: Int,
    next: List[String],
    previous: List[String],
    color: Option[String],
    resources: List[String]
  );

  case class UiMachine(
    name: String,
    default: Int,
    min: Int,
    max: Int
  );

  case class RetoolMachines(
    id: List[Int],
    name: List[String],
    minrep: List[Int],
    maxrep: List[Int],
    defrep: List[Int],
    taskid: List[Int]
  )

  case class RetoolSingleMachine(id: Int, name: String, minrep: Int, maxrep: Int, defrep: Int, taskid: Int)

  case class RetoolEdge(id: List[Int], idfrom: List[Int], idto: List[Int], taskid: List[String])

  case class SingleRetoolEdge(id: Int, idfrom: Int, idto: Int, taskid: String)

  case class RetoolSchedule(
    id: List[Int],
    name: List[String],
    makespan: List[String],
    machine: List[String],
    taskid: List[Int]
  )

  case class SingleRetoolTask(id: Int, name: String, makespan: String, machine: String, taskid: Int)

  case class RetoolInput(
    machines: RetoolMachines,
    schedule: RetoolSchedule,
    edges: RetoolEdge,
    human: String,
    ndays: String,
    shiftresolution: String,
    topk: Option[String],
    maxshifttime: String,
    machinekupperbound: String,
    machineklowerbound: String,
    iterations: String,
    scenario: String,
    toPerturbate: String,
    displayshift: Boolean
  )

  /**
   * The schedule of a particular replica
   * @param id - id of replica for gantt e.g. 143_2
   * @param name - name of replica for gantt, e.g. Human_2
   * @param activities - activities for gantt
   * @param machineId- the originalID of the machine e.g. 143
   * @param replicaIndex - the index of the replica e.g. 2
   * @param machineName - the original name of the machine, e.g Human
   */
  case class MachineSchedules(
    id: String,
    name: String,
    activities: List[MachineScheduleActivity], //Do not change names, used by ganttchart js
    machineId: Int,
    replicaIndex: Int,
    machineName: String
  )

  case class MachineScheduleActivity(
    id: String,
    name: String,
    start: Long,
    end: Long, //Do not change names, used by ganttchart js
    operationId: Int
  ) //TODO add operation ID, make ID unique

  case class PossibleSchedule(
    data: List[MachineSchedules],
    machinesTableEntry: List[MachineTableEntry],
    replicas: List[MachineReplicaTableEntry],
    shifts: List[Shift]
  ) //Do not change names, used by ganttchart js

  case class Shift(time: Long) //Do not change names, used by ganttchart js

  case class MachineTableEntry(name: String, replicas: Int, replicasInUse: Int, totalTime: Long) //Do not change names, used by ganttchart js

  case class MachineReplicaTableEntry(name: String, nTasks: Int, timeInUse: Long) //Do not change names, used by ganttchart js

  sealed trait ScheduleGenResponse
  case class ScheduleGenResponseRetool(
    mapMakespanListSchedule: Map[Long, List[PossibleSchedule]], //Schedules for each span
    makeSpanKeys: List[Long], //redundant. Could be taken from above values. Helps the UI
    scenario: String, //Name of the scenario
    input: RetoolInput, //The input that was given from the retool UI
    scatterPlot: RetoolPlotlyPlotWithMetadata //The data that will be sent to the scatter plot
  ) extends ScheduleGenResponse

  case class ScheduleGenResponseUi(
    mapMakespanListSchedule: Map[Long, List[PossibleSchedule]], //Schedules for each span
    makeSpanKeys: List[Long], //redundant. Could be taken from above values. Helps the UI
    scenario: String, //Name of the scenario
    input: UiInput, //The input that was given from the retool UI
    scatterPlot: RetoolPlotlyPlotWithMetadata //The data that will be sent to the scatter plot)
  ) extends ScheduleGenResponse

  class RetoolPlotlyPlot(defaultX: String, defaultY: String, data: Map[String, List[Double]])
  case class RetoolPlotlyPlotWithMetadata(
    defaultX: String,
    defaultY: String,
    data: Map[String, List[Double]],
    metadata: List[RetoolPlotlyMetaData],
    size: List[Double],
    color: List[String]
  ) extends RetoolPlotlyPlot(defaultX, defaultY, data)

  /**
   *
  //This is used by retool to allow clicking. If you click a point
   it will load the point associated with this time and index
   We use time and index since that is how the data is sent, as a map of time to list of schedules
   */
  case class RetoolPlotlyMetaData(time: Long, index: Int)

}

object Serializers {
  import Protocol._

  implicit def mapSchemaForLong[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[Long, V]] = {
    new SchemaFor[Map[Long, V]] {
      override def schema: Schema =
        SchemaBuilder.map().values(schemaFor.schema)
      override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

    }
  }

  implicit def mapDecoderLong[T](implicit valueDecoder: Decoder[T], schemaForImp: SchemaFor[T]): Decoder[Map[Long, T]] =
    new Decoder[Map[Long, T]] {

      override def decode(value: Any): Map[Long, T] =
        value match {
          case map: java.util.Map[_, _] =>
            map.asScala.toMap.map {
              case (k, v) =>
                implicitly[Decoder[Long]]
                  .decode(k) -> valueDecoder.decode(v)
            }
          case other => sys.error("Unsupported map " + other)
        }
      override def schemaFor: SchemaFor[Map[Long, T]] = mapSchemaForLong[T]

    }

  implicit def mapEncoderLong[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[Long, V]] =
    new Encoder[Map[Long, V]] {

      override def encode(map: Map[Long, V]): java.util.Map[String, AnyRef] = {
        require(schema != null)
        val java = new util.HashMap[String, AnyRef]
        map.foreach {
          case (k, v) =>
            java.put(k.toString, encoder.encode(v))
        }
        java
      }
      override def schemaFor: SchemaFor[Map[Long, V]] = mapSchemaForLong[V]

    }

  class ScheduleGenResponseSerializer extends AvroSerializer[ScheduleGenResponse]
  class ScheduleGenResponseSerializerRetool extends AvroSerializer[ScheduleGenResponseRetool]
  class ScheduleGenResponseSerializerUi extends AvroSerializer[ScheduleGenResponseUi]

  class RetoolInputSerializer extends AvroSerializer[RetoolInput]

  class InputBunnySerializer(implicit A: ExtendedActorSystem) extends AvroSerializer[InputBunny]
  class InputBunnyRetoolSerializer(implicit A: ExtendedActorSystem) extends AvroSerializer[InputBunnyRetool]
  class InputBunnyUiSerializer(implicit A: ExtendedActorSystem) extends AvroSerializer[InputBunnyUi]
}
