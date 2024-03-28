package com.radix.shared.persistence

import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import akka.actor.typed.ActorRef
import org.apache.avro.{Schema, SchemaBuilder}
import akka.actor.{ActorRef => UActorRef, ExtendedActorSystem}
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.Serialization
import akka.stream.{SinkRef, SourceRef, StreamRefResolver}

object ActorRefSerializer {
  implicit def SchemaForTypedActorRef[T]: SchemaFor[ActorRef[T]] = new SchemaFor[ActorRef[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }
  implicit def EncoderForTypedActorRef[T]: Encoder[ActorRef[T]] = new Encoder[ActorRef[T]] {
    override def encode(t: ActorRef[T]): AnyRef = {
      Serialization.serializedActorPath(t.toClassic)
    }
    override def schemaFor: SchemaFor[ActorRef[T]] = SchemaForTypedActorRef[T]
  }
  implicit def DecoderForTypedActorRef[T](implicit A: ExtendedActorSystem): Decoder[ActorRef[T]] =
    new Decoder[ActorRef[T]] {
      override def decode(value: Any): ActorRef[T] = {
        A.provider.resolveActorRef(value.toString)
      }
      override def schemaFor: SchemaFor[ActorRef[T]] = SchemaForTypedActorRef[T]

    }

  implicit object SchemaForUntypedActorRef extends SchemaFor[UActorRef] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }
  implicit object EncoderForUntypedActorRef extends Encoder[UActorRef] {
    override def encode(t: UActorRef): AnyRef =
      Serialization.serializedActorPath(t)

    override def schemaFor: SchemaFor[UActorRef] = SchemaForUntypedActorRef
  }
  implicit def DecoderForUntypedActorRef(implicit A: ExtendedActorSystem): Decoder[UActorRef] = new Decoder[UActorRef] {
    override def decode(value: Any): UActorRef = {
      A.provider.resolveActorRef(value.toString)
    }
    override def schemaFor: SchemaFor[UActorRef] = SchemaForUntypedActorRef

  }

  implicit def SchemaForNothingActorRef: SchemaFor[ActorRef[_]] = new SchemaFor[ActorRef[_]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }
  implicit def EncoderForNothingActorRef: Encoder[ActorRef[_]] = new Encoder[ActorRef[_]] {
    override def encode(t: ActorRef[_]): AnyRef = {
      Serialization.serializedActorPath(t.toClassic)
    }
    override def schemaFor: SchemaFor[ActorRef[_]] = SchemaForNothingActorRef
  }

  implicit def SchemaForSourceRef[T]: SchemaFor[SourceRef[T]] = new SchemaFor[SourceRef[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit def EncoderForSourceRef[T](implicit eas: ExtendedActorSystem): Encoder[SourceRef[T]] =
    new Encoder[SourceRef[T]] {
      override def encode(ref: SourceRef[T]): AnyRef =
        StreamRefResolver.get(eas).toSerializationFormat(ref)
      override def schemaFor: SchemaFor[SourceRef[T]] = SchemaForSourceRef

    }
  implicit def DecoderForSourceRef[T](implicit eas: ExtendedActorSystem): Decoder[SourceRef[T]] =
    new Decoder[SourceRef[T]] {
      override def decode(value: Any): SourceRef[T] =
        StreamRefResolver.get(eas).resolveSourceRef(value.toString)
      override def schemaFor: SchemaFor[SourceRef[T]] = SchemaForSourceRef
    }

  implicit def SchemaForSinkRef[T]: SchemaFor[SinkRef[T]] = new SchemaFor[SinkRef[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit def EncoderForSinkRef[T](implicit eas: ExtendedActorSystem): Encoder[SinkRef[T]] =
    new Encoder[SinkRef[T]] {
      override def encode(ref: SinkRef[T]): AnyRef =
        StreamRefResolver.get(eas).toSerializationFormat(ref)
      override def schemaFor: SchemaFor[SinkRef[T]] = SchemaForSinkRef

    }
  implicit def DecoderForSinkRef[T](implicit eas: ExtendedActorSystem): Decoder[SinkRef[T]] =
    new Decoder[SinkRef[T]] {
      override def decode(value: Any): SinkRef[T] =
        StreamRefResolver.get(eas).resolveSinkRef(value.toString)
      override def schemaFor: SchemaFor[SinkRef[T]] = SchemaForSinkRef
    }
}
