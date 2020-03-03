package com.radix.shared.persistence

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import akka.actor.typed.ActorRef
import org.apache.avro.{Schema, SchemaBuilder}
import akka.actor.{ExtendedActorSystem, ActorRef => UActorRef}
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serialization

object ActorRefSerializer {
  implicit def SchemaForTypedActorRef[T]: SchemaFor[ActorRef[T]] = new SchemaFor[ActorRef[T]] {
    override def schema(fieldMapper: FieldMapper): Schema = SchemaBuilder.builder.stringType()
  }
  implicit def EncoderForTypedActorRef[T]: Encoder[ActorRef[T]] = new Encoder[ActorRef[T]] {
    override def encode(t: ActorRef[T], schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      Serialization.serializedActorPath(t.toUntyped)
    }
  }
  implicit def DecoderForTypedActorRef[T](implicit A: ExtendedActorSystem): Decoder[ActorRef[T]] = new Decoder[ActorRef[T]] {
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): ActorRef[T] = {
      A.provider.resolveActorRef(value.toString)
    }
  }

  implicit object SchemaForUntypedActorRef extends SchemaFor[UActorRef] {
    override def schema(fieldMapper: FieldMapper): Schema = SchemaBuilder.builder.stringType()
  }
  implicit object EncoderForUntypedActorRef extends Encoder[UActorRef] {
    override def encode(t: UActorRef, schema: Schema, fieldMapper: FieldMapper): AnyRef =
      Serialization.serializedActorPath(t)
  }
  implicit def DecoderForUntypedActorRef(implicit A: ExtendedActorSystem): Decoder[UActorRef] = new Decoder[UActorRef] {
    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): UActorRef = {
      A.provider.resolveActorRef(value.toString)
    }
  }
}