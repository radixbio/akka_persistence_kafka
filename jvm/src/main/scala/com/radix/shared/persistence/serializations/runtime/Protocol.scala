package com.radix.shared.persistence.serializations.runtime

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist.Listing
import akka.actor.typed.receptionist.ServiceKey
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ServiceKeySerializer._
import com.radix.shared.persistence.ActorRefSerializer._
import com.sksamuel.avro4s._
import org.apache.avro.Schema

object Protocol {
  //unsealed due to private extension inside runtime
  //TODO fix this with message adapters to seal this up
  trait Request
  sealed trait Response
  final case class AtLeastOne[T](skey: ServiceKey[T], replyTo: ActorRef[AtLeastOneResponse[T]]) extends Request
  final case class AtLeastOneMulti(skeys: Set[ServiceKey[_]], replyTo: ActorRef[AtLeastOneMultiResponse])
      extends Request
  final case class Read[T](skey: ServiceKey[T], replyTo: ActorRef[ReadResponse[T]]) extends Request
  final case class ReadMulti(skeys: Set[ServiceKey[_]], replyTo: ActorRef[ReadMultiResponse]) extends Request
  final case class AtLeastOneResponse[T](skey: Set[ActorRef[T]]) extends Response
  final case class AtLeastOneMultiResponse(refs: Map[ServiceKey[_], Set[ActorRef[_]]]) extends Response
  final case class ReadResponse[T](refs: Set[ActorRef[T]]) extends Response
  final case class ReadMultiResponse(skey: Map[ServiceKey[_], Set[ActorRef[_]]]) extends Response

}

object serializers {
  import Protocol._

  implicit object MapSchema extends SchemaFor[Map[ServiceKey[_], Set[ActorRef[_]]]] {
    override def schema: Schema = implicitly[SchemaFor[Map[String, Set[ActorRef[_]]]]].schema
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }
  implicit object MapEncoder extends Encoder[Map[ServiceKey[_], Set[ActorRef[_]]]] {
    override def encode(t: Map[ServiceKey[_], Set[ActorRef[_]]]): AnyRef = {
      val S = implicitly[Encoder[Map[String, Set[ActorRef[_]]]]]
      S.encode(t.map({ case ((k, v)) => (k.id, v) }))
    }

    override def schemaFor: SchemaFor[Map[ServiceKey[_], Set[ActorRef[_]]]] = MapSchema
  }
  implicit def MapDecoder(implicit as: ExtendedActorSystem): Decoder[Map[ServiceKey[_], Set[ActorRef[_]]]] =
    new Decoder[Map[ServiceKey[_], Set[ActorRef[_]]]] {
      override def decode(value: Any): Map[ServiceKey[_], Set[ActorRef[_]]] = {
        //DecoderForTypedActorRef[_]
        //implicitly[Decoder[ActorRef[Int]]]
        //val S = implicitly[Decoder[Map[String, Set[ActorRef[_]]]]]
        //S.decode(value, schema, fieldMapper).map({case (k, v) => (ServiceKey(k), v)})
        ???
      }
      override def schemaFor: SchemaFor[Map[ServiceKey[_], Set[ActorRef[_]]]] = MapSchema

    }

  class AtLeastOneAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[AtLeastOne[Any]]
  class AtLeastOneMultiAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[AtLeastOneMulti]
  class ReadAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[Read[Any]]
  class ReadMultiAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[ReadMulti]
  class AtLeastOneResponseAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[AtLeastOneResponse[Any]]
  class AtLeastOneMultiResponseAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[AtLeastOneMultiResponse]
  class ReadResponseAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[ReadResponse[Any]]
  class ReadMultiResponseAvro(implicit as: ExtendedActorSystem) extends AvroSerializer[ReadMultiResponse]

}
