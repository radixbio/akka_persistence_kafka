package com.radix.shared.persistence.serializations.utils.healthcheck

import akka.actor.typed.ActorRef
import akka.actor.{ExtendedActorSystem}
import akka.serialization.Serialization
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}

object Protocol {

  sealed trait HealthCheckRequest
  case object HealthCheckUpdateChecks extends HealthCheckRequest
  case class HealthCheckSubscribe(replyTo: ActorRef[Any]) extends HealthCheckRequest

  sealed trait HealthCheckResponse
  case class HealthCheckUpdate(checks: String) extends HealthCheckResponse

  final case class HealthCheckState(subscribers: Set[ActorRef[Any]])

  object HealthCheckState {
    def empty: HealthCheckState = HealthCheckState(Set.empty)
  }

}

object Serializers {

  import Protocol._

  class AvroHealthCheckSubscribe(implicit eas: ExtendedActorSystem) extends AvroSerializer[HealthCheckSubscribe]
  class AvroHealthCheckUpdate extends AvroSerializer[HealthCheckUpdate]
}
