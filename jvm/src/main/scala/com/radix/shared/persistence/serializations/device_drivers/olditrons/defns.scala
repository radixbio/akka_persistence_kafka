package com.radix.shared.persistence.serializations.device_drivers.olditrons

import java.time.Instant

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory

import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.device_drivers.elemental_radix_driver.ElementalRadixDriverTypes.{ElementalDriverable, ElementalSendable}
import io.circe.Json

object defns {

  type ReplyToActor = ActorRef[OlditronsResponse]

  sealed trait OlditronsRequest {
    def replyTo: Option[ReplyToActor]
  }
  sealed trait OlditronsResponse
  sealed trait OlditronsEvent

  case class SummaryRequest(replyTo: Option[ReplyToActor]) extends OlditronsRequest
  case class SummaryResponse(summary: OlditronsSummary) extends OlditronsResponse with ElementalSendable {
    override def packets: List[ElementalDriverable] = List(summary)
  }
  case class SummaryGenerated(summary: OlditronsSummary, errors: List[String]) extends OlditronsEvent

  /**
   * These older Multitrons protocol devices only report temp and speed, it seems (n=1).
   * @param unitID The unit ID in the stack + the device ID. See the datasheet for details. Some of the addresses weren't written in the firmware, so take it with a grain of salt. (If it's "0", that's why.)
   * @param rpm Canonically the "speed" with the newer units. How fast the unit is spinning/shaking. Note that the datasheet says this can only go up to 400; that's a lie, it can go up to 999 and can actually go into the low 1000s.
   * @param temp The temperature inside the incubator.
   * @param uidadd UUID postfix for Elemental.
   * @param time The time that we read in this data.
   */
  case class OlditronsSummary(
    unitID: Option[String],
    rpm: Option[String],
    temp: Option[String],
    uidadd: String,
    time: Instant
  ) extends ElementalDriverable {

    override def toElementalJSON: Json = {
      val forjson: Map[String, Option[String]] = Map[String, Option[String]](
        "temperature" -> temp,
        "speed" -> rpm,
        "unit_id" -> unitID,
        "mac_address" -> Some(uuid + this.uidadd),
        "timestamp" -> Some(this.time.getEpochSecond.toString)
      )

      val jsonable = forjson.filter(e => e._2.isDefined)

      println(s"jsons filtered in OlditronsSum: $jsonable")

      val json = jsonable.map(p => Tuple2(p._1, Json.fromString(p._2.getOrElse("ERROR"))))

      Json.fromFields(json.toList)
    }

    override def uidPostfix: String = "OLD"
  }

  object OlditronsSummary {

    def apply(
      unitID: Option[String],
      rpm: Option[String],
      temp: Option[String],
      uidadd: String = "OLD",
      time: Instant = Instant.now()
    ): OlditronsSummary = new OlditronsSummary(unitID, rpm, temp, uidadd, time)
  }

  class SummaryRequestSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[SummaryRequest]
  class OlditronsSummarySerializer extends AvroSerializer[OlditronsSummary]
  class SummaryResponseSerializer extends AvroSerializer[SummaryResponse]
  class SummaryGeneratedSerializer extends AvroSerializer[SummaryGenerated]
}
