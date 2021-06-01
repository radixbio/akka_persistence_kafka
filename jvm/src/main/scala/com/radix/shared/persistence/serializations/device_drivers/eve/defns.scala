package com.radix.shared.persistence.serializations.device_drivers.eve

import java.time.Instant

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.device_drivers.eve.EveUnit.parseEveUnit
import io.circe
import io.circe._
import io.circe.syntax.EncoderOps
import shared.circe_custom_decoders.src.main.scala.Decoders.decodeDoubleOrNaN

object defns {

  sealed trait EveEvent

  case class EveCompletedRequest(request: EveCommunicationRequest, response: EveResponse)
      extends EveEvent
      with EveRequest

  class EveCompletedRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[EveCompletedRequest]

  /**
   * A request that is sent to the Eve driver.
   */
  sealed trait EveRequest

  /**
   * Common supertype for all valid responses that Eve could have to a request.
   */
  sealed trait EveResponse

  /**
   * A request that is sent to communicate with Eve (as opposed to being a message that the driver sends to itself).
   */
  sealed trait EveCommunicationRequest extends EveRequest {

    /**
     * Actor to send response to once request goes through.
     */
    val replyTo: Option[ActorRef[EveResponse]]
  }

  /**
   * A RESTful endpoint that can be used to contact Eve.
   */
  sealed trait EveGetRequest extends EveCommunicationRequest

  /**
   * Request that sends data from client to Eve in its body.
   *
   * @tparam T The type of data that is being sent to Eve.
   */
  sealed trait EvePostRequest[T] extends EveCommunicationRequest {

    /**
     * The body of the request to be sent to Eve.
     */
    val body: T

  }

  /**
   * Gets all the batches stored in Eve.
   */
  case class GetAllBatches(
    replyTo: Option[ActorRef[EveResponse]]
  ) extends EveGetRequest

  class GetAllBatchesSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetAllBatches]

  /**
   * Get a specific batch stored in Eve
   */
  case class GetBatchByID(replyTo: Option[ActorRef[EveResponse]], batchID: String) extends EveGetRequest

  class GetBatchByIDSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetBatchByID]

  /**
   * Get all of the datapoints associated with a batch.
   */
  case class GetBatchDatapoints(replyTo: Option[ActorRef[EveResponse]], batchID: String) extends EveGetRequest

  class GetBatchDatapointsSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GetBatchDatapoints]

  /**
   * Create a batch with the given information. The name of the batch must be unique.
   */
  case class CreateBatch(replyTo: Option[ActorRef[EveResponse]], body: PostableEveBatch)
      extends EvePostRequest[PostableEveBatch]

  class CreateBatchSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CreateBatch]

  /**
   * Request to create a parameter for the given batch.
   */
  case class CreateBatchParameter(
    replyTo: Option[ActorRef[EveResponse]],
    batchID: String,
    body: PostableEveParameter
  ) extends EvePostRequest[PostableEveParameter]

  class CreateBatchParameterSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CreateBatchParameter]

  /**
   * Adds values to a batch's parameter of the Eve API.
   */
  case class AddBatchParameterValue(
    replyTo: Option[ActorRef[EveResponse]],
    batchID: String,
    parameterID: String,
    body: List[PostableEveDatapoint]
  ) extends EvePostRequest[List[PostableEveDatapoint]]

  class AddBatchParameterValueSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[AddBatchParameterValue]

  /**
   * A culture medium used in a batch
   *
   * @param name        Human-readable name of culture media
   * @param description Human-readable description of culture media
   * @param cultureType Type of substance in culture media
   * @param producer    Producer of culture media
   * @param volume      Volume of culture media used
   * @param volumeUnit  The unit used to measure the culture media's volume
   * @param ph          The PH of the culture media
   * @param metadata    Any metadata associated with the culture media
   */
  case class EveCultureMedia(
    name: String,
    description: String,
    cultureType: String,
    producer: String,
    volume: Double,
    volumeUnit: EveUnit,
    ph: Double,
    metadata: List[EveMetadata]
  )

  class EveCultureMediaSerializer extends AvroSerializer[EveCultureMedia]

  object EveCultureMedia {
    implicit val decode: circe.Decoder[EveCultureMedia] = (c: HCursor) => {
      for {
        name <- c.downField("Name").as[String]
        description <- c.downField("Description").as[String]
        cultureType <- c.downField("Type").as[String]
        producer <- c.downField("Producer").as[String]
        volume <- c.downField("Volume").as[Double]
        volumeUnit <- c.downField("VolumeUnit").as[String]
        ph <- c.downField("PH").as[Double]
        metadata <- c.downField("Metadata").as[List[EveMetadata]]
      } yield {
        EveCultureMedia(name, description, cultureType, producer, volume, parseEveUnit(volumeUnit), ph, metadata)
      }
    }
  }

  /**
   * Datapoint that can be posted to Eve.
   *
   * @param elapsedSeconds Number of seconds since the start of the batch when datapoint was captured.
   * @param value          The numeric value of the datapoint.
   */
  case class PostableEveDatapoint(elapsedSeconds: Int, value: Double)

  class PostableEveDatapointSerializer extends AvroSerializer[PostableEveDatapoint]

  object PostableEveDatapoint {
    implicit val encoder: Encoder[PostableEveDatapoint] = (p: PostableEveDatapoint) => {
      Json.obj(
        ("RecordTime", Json.fromInt(p.elapsedSeconds)),
        ("Value", Json.fromDoubleOrNull(p.value))
      )
    }
  }

  /**
   * Stores a single piece of Eve metadata.
   */
  case class EveMetadata(name: String, value: String, unit: EveUnit)

  class EveMetadataSerializer extends AvroSerializer[EveMetadata]

  object EveMetadata {

    implicit val decode: io.circe.Decoder[EveMetadata] = (c: HCursor) => {
      for {
        name <- c.downField("Name").as[String]
        value <- c.downField("Value").as[String]
        unit <- c.downField("Unit").as[String]
      } yield {
        EveMetadata(name, value, parseEveUnit(unit))
      }
    }
  }

  /**
   * Represents organism being studied in an Eve batch
   *
   * @param name        Name of organism
   * @param group       Name of group that organism belongs to
   * @param cloneName   Name of the clone of the organism being studied
   * @param cloneId     ID of the clone of the organism being studied
   * @param description Human-readable description of the organism
   * @param metadata    Any metadata associated with the organism
   */
  case class EveOrganism(
    name: String,
    group: String,
    cloneName: String,
    cloneId: String,
    description: String,
    metadata: List[EveMetadata]
  )

  class EveOrganismSerializer extends AvroSerializer[EveOrganism]

  object EveOrganism {
    implicit val decoder: Decoder[EveOrganism] = (c: HCursor) => {
      for {
        name <- c.downField("Name").as[String]
        group <- c.downField("Group").as[String]
        clone <- c.downField("Clone").as[String]
        cloneId <- c.downField("CloneId").as[String]
        desc <- c.downField("Description").as[String]
        meta <- c.downField("Metadata").as[List[EveMetadata]]
      } yield {
        EveOrganism(name, group, clone, cloneId, desc, meta)
      }
    }
  }

  /**
   * Tracks a parameter being measured in a batch
   *
   * @param id   Unique GUID of parameter
   * @param name Human-readable name of parameter
   * @param unit Unit that parameter is measured in
   */
  case class EveParameter(id: String, name: String, unit: EveUnit)

  class EveParameterSerializer extends AvroSerializer[EveParameter]

  object EveParameter {
    implicit val decoder: Decoder[EveParameter] = { c =>
      for {
        id <- c.downField("Id").as[String]
        name <- c.downField("Name").as[String]
        unit <- c.downField("Unit").as[String]
      } yield {
        EveParameter(id, name, parseEveUnit(unit))
      }
    }
    implicit val encoder: Encoder[EveParameter] = (p: EveParameter) => {
      Json.obj(
        ("Id", Json.fromString(p.id)),
        ("Name", Json.fromString(p.name)),
        ("Unit", Json.fromString(p.unit.fullName))
      )
    }
  }

  /**
   * Contains information relating to EveParameters that Eve will accept in post requests. Eve will generate and assign
   * a GUID.
   *
   * @param name The human-readable name of the parameter.
   * @param unit The unit for the parameter to be measured in.
   */
  case class PostableEveParameter(name: String, unit: EveUnit)

  class PostableEveParameterSerializer extends AvroSerializer[PostableEveParameter]

  object PostableEveParameter {
    implicit val encode: Encoder[PostableEveParameter] = (p: PostableEveParameter) => {
      Json.obj(
        ("Name", Json.fromString(p.name)),
        ("Unit", Json.fromString(p.unit.fullName))
      )
    }
  }

  /**
   * Stores data about a batch tracked by Eve.
   *
   * @param inoculationTime Time in seconds from the start of the batch to the inoculation time.
   * @param device          The name of the machine that the batch is being run on.
   * @param phaseRecords    A list of the different phases of the batch.
   */
  case class EveBatch(
    id: String,
    name: String,
    description: String,
    experiment: String,
    project: String,
    recipe: String,
    startTime: Option[Instant],
    endTime: Option[Instant],
    duration: Option[Int],
    state: EveState,
    metadata: List[EveMetadata],
    parameters: List[EveParameter],
    organism: Option[EveOrganism],
    cultureMedia: List[EveCultureMedia],
    inoculationTime: Option[Int],
    device: String,
    phaseRecords: List[EvePhaseRecord]
  ) extends EveResponse {

    /**
     * Converts batch to a formatted string.
     */
    def printBatchNice: String = {
      "Batch Info: \n" +
        s"\t ID: ${id}\n" +
        s"\t Name: ${name}\n" +
        s"\t Description: ${description}\n" +
        s"\t Experiment: ${experiment}\n" +
        s"\t Project: ${project}\n" +
        s"\t Recipe: ${recipe}\n" +
        s"\t StartTime: ${startTime}\n" +
        s"\t EndTime: ${endTime}\n" +
        s"\t Duration: ${duration}\n" +
        s"\t State: ${state}\n" +
        s"\t Metadata: ${metadata}\n" +
        s"\t Parameters: \n${parameters.map(p => s"\t\t ID: ${p.id}, NAME: ${p.name}, UNIT: ${p.unit}\n")}" +
        s"\t Organism: \n${organism.map(o =>
          s"\t\t Name: ${o.name}\n" +
            s"\t\t Group: ${o.group}\n" +
            s"\t\t cloneName: ${o.cloneName}\n" +
            s"\t\t cloneID: ${o.cloneId}\n" +
            s"\t\t description: ${o.description}\n" +
            s"\t\t metadata: ${o.metadata}\n"
        )}" +
        s"\t CultureMedia: \n${cultureMedia.map(m =>
          s"\t\t Name: ${m.name}\n" +
            s"\t\t Description: ${m.description}\n" +
            s"\t\t Type: ${m.cultureType}\n" +
            s"\t\t Producer: ${m.producer}\n" +
            s"\t\t Volume: ${m.volume}\n" +
            s"\t\t VolumeUnit: ${m.volumeUnit}\n" +
            s"\t\t PH: ${m.ph}\n" +
            s"\t\t Metadata: ${m.metadata}\n" +
            s"\t\t VVVVVVVVVVVVVVVVVVVVVV\n"
        )}" +
        s"\t InoculationTime: ${inoculationTime}\n" +
        s"\t Device: ${device}\n" +
        s"\t PhaseRecords: \n${phaseRecords.map(r =>
          s"\t\t Name: ${r.name}\n" +
            s"\t\t Start Time: ${r.startTime}\n" +
            s"\t\t VVVVVVVVVVVVVVVVVVVVVVVVVV\n"
        )}"
    }
  }

  class EveBatchSerializer extends AvroSerializer[EveBatch]

  object EveBatch {
    implicit val decode: Decoder[EveBatch] = (c: HCursor) => {
      for {
        id <- c.downField("Id").as[String]
        name <- c.downField("Name").as[String]
        description <- c.downField("Description").as[Option[String]]
        experiment <- c.downField("Experiment").as[String]
        project <- c.downField("Project").as[String]
        recipe <- c.downField("Recipe").as[Option[String]]
        startTime <- c.downField("StartDateTime").as[Option[Instant]]
        duration <- c.downField("Duration").as[Option[Int]]
        inoculationtime <- c.downField("InoculationTime").as[Option[Int]]
        state <- c.downField("State").as[String]
        device <- c.downField("Device").as[String]
        metadata <- c.downField("Metadata").as[List[EveMetadata]]
        records <- c.downField("PhaseRecords").as[List[EvePhaseRecord]]
        params <- c.downField("Parameters").as[Option[List[EveParameter]]]
        organism <- c.downField("Organism").as[Option[EveOrganism]]
        culture <- c.downField("CultureMedia").as[Option[List[EveCultureMedia]]]
      } yield {
        EveBatch(
          id,
          name,
          description.getOrElse(""),
          experiment,
          project,
          recipe.getOrElse(""),
          startTime,
          if (startTime.isDefined && duration.isDefined) Some(startTime.get.plusSeconds(duration.get)) else None,
          duration,
          EveState.decodeFromString(state),
          metadata,
          params.getOrElse(List[EveParameter]()),
          organism,
          culture.getOrElse(List[EveCultureMedia]()),
          inoculationtime,
          device,
          records
        )
      }
    }
  }

  /**
   * Data that can be posted to Eve to create a batch
   */
  case class PostableEveBatch(
    name: String,
    startDateTime: Instant,
    duration: Int,
    parameters: List[PostableEveParameter]
  ) {
    if (duration < 1) {
      throw new IllegalArgumentException("Batch duration must be greater than or equal to 1!")
    }
    try {
      startDateTime.toEpochMilli
    } catch {
      case _: ArithmeticException =>
        throw new IllegalArgumentException(
          s"StartDateTime of value $startDateTime is " +
            s"too big to be serialized properly!"
        )
    }
  }

  class PostableEveBatchSerializer extends AvroSerializer[PostableEveBatch]

  object PostableEveBatch {
    implicit val encodeBatch: Encoder[PostableEveBatch] = (a: PostableEveBatch) =>
      Json.obj(
        ("Name", Json.fromString(a.name)),
        ("StartDateTime", a.startDateTime.asJson),
        ("Duration", Json.fromInt(a.duration)),
        ("Parameters", a.parameters.asJson)
      )
  }

  /**
   * Wrapper for lists of EveBatch
   */
  case class EveBatchList(list: List[EveBatch]) extends EveResponse

  class EveBatchListSerializer extends AvroSerializer[EveBatchList]

  object EveBatchList {
    implicit val decoder: Decoder[EveBatchList] = { c =>
      c.as[List[EveBatch]].map(EveBatchList(_))
    }
  }

  /**
   * Represents a specific phase of a batch
   *
   * @param name      The name of the phase
   * @param startTime The number of seconds since the batch started when the phase started
   */
  case class EvePhaseRecord(name: String, startTime: Int) extends EveResponse

  class EvePhaseRecordSerializer extends AvroSerializer[EvePhaseRecord]

  object EvePhaseRecord {
    implicit val decoder: Decoder[EvePhaseRecord] = (c: HCursor) => {
      for {
        name <- c.downField("Name").as[String]
        time <- c.downField("StartTime").as[Int]
      } yield {
        EvePhaseRecord(name, time)
      }
    }
  }

  /**
   * A single unit of data stored in Eve,
   */
  case class EveDatapoint(
    elapsedSeconds: Int,
    value: Double,
    parameterId: String,
    parameterName: String,
    phaseName: String
  )

  class EveDatapointSerializer extends AvroSerializer[EveDatapoint]

  object EveDatapoint {
    implicit val decode: Decoder[EveDatapoint] = (c: HCursor) => {
      for {
        seconds <- c.downField("RecordTime").as[Int]
        value <- c.downField("Value").as[Double]
        pid <- c.downField("ParameterId").as[String]
        pname <- c.downField("ParameterName").as[String]
        phaseName <- c.downField("PhaseName").as[String]
      } yield {
        EveDatapoint(seconds, value, pid, pname, phaseName)
      }
    }
  }

  /**
   * Wrapper for lists of EveDatapoint.
   */
  case class EveDatapointList(list: List[EveDatapoint]) extends EveResponse

  class EveDatapointListSerializer extends AvroSerializer[EveDatapointList]

  object EveDatapointList {
    implicit val decoder: Decoder[EveDatapointList] = Decoder.decodeList[EveDatapoint].map(EveDatapointList(_))
  }

  /**
   * A new batch was successfully created.
   * @param id The ID assigned to the batch by Eve.
   */
  case class BatchCreated(id: String) extends EveResponse

  class BatchCreatedSerializer extends AvroSerializer[BatchCreated]

  object BatchCreated {
    implicit val decoder: Decoder[BatchCreated] = { c =>
      c.downField("Id").as[String].map(BatchCreated(_))
    }
  }

  /**
   * A new parameter was successfully created
   * @param id The ID assigned to the parameter by Eve.
   */
  case class ParameterCreated(id: String) extends EveResponse

  class ParameterCreatedSerializer extends AvroSerializer[ParameterCreated]

  object ParameterCreated {
    implicit val decoder: Decoder[ParameterCreated] = { c =>
      c.downField("Id").as[String].map(ParameterCreated(_))
    }
  }

  /**
   * The requested datapoints were successfully added to Eve.
   * Eve will usually send this message prematurely, so the added datapoints will not be available in Eve for a
   * few seconds.
   */
  case class DataAdded() extends EveResponse

  class DataAddedSerializer extends AvroSerializer[DataAdded]

  object DataAdded {
    implicit val decoder: Decoder[DataAdded] = { _ =>
      Right(DataAdded())
    }
  }

  /**
   * A response indicating that a request could not be successfully completed.
   */
  sealed trait FailedRequest extends EveResponse

  case object EveOffline extends FailedRequest

  case object RESTDisabled extends FailedRequest

  /**
   * A response indicating that a request could not be successfully completed, specifically because of invalid data.
   */
  sealed trait RejectedRequest extends EveResponse

  case class BatchDoesNotExist(batchID: String) extends RejectedRequest

  class BatchDoesNotExistSerializer extends AvroSerializer[BatchDoesNotExist]

  case class ParameterDoesNotExist(parameterID: String) extends RejectedRequest

  class ParameterDoesNotExistSerializer extends AvroSerializer[ParameterDoesNotExist]

  case class BatchAlreadyExists(name: String) extends RejectedRequest

  class BatchAlreadyExistsSerializer extends AvroSerializer[BatchAlreadyExists]

  case class InvalidBatchData(batchName: String, reason: String) extends RejectedRequest

  class InvalidBatchDataSerializer extends AvroSerializer[InvalidBatchData]

}
