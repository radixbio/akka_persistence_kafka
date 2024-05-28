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
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit def EncoderForTypedActorRef[T]: Encoder[ActorRef[T]] = new Encoder[ActorRef[T]] {
    override def encode(schema: Schema): ActorRef[T] => AnyRef = { (t: ActorRef[T]) =>
      Serialization.serializedActorPath(t.toClassic)
    }
  }
  implicit def DecoderForTypedActorRef[T](implicit A: ExtendedActorSystem): Decoder[ActorRef[T]] =
    new Decoder[ActorRef[T]] {
      override def decode(schema: Schema): Any => ActorRef[T] = { (value: Any) =>
        A.provider.resolveActorRef(value.toString)
      }

    }

  implicit def SchemaForUntypedActorRef: SchemaFor[UActorRef] = new SchemaFor[UActorRef] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit object EncoderForUntypedActorRef extends Encoder[UActorRef] {
    override def encode(schema: Schema): UActorRef => AnyRef =
      (t: UActorRef) => Serialization.serializedActorPath(t)

  }
  implicit def DecoderForUntypedActorRef(implicit A: ExtendedActorSystem): Decoder[UActorRef] = new Decoder[UActorRef] {
    override def decode(schema: Schema): Any => UActorRef = { (value: Any) =>
      A.provider.resolveActorRef(value.toString)
    }

  }

  implicit def SchemaForNothingActorRef: SchemaFor[ActorRef[_]] = new SchemaFor[ActorRef[_]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit def EncoderForNothingActorRef: Encoder[ActorRef[_]] = new Encoder[ActorRef[_]] {
    override def encode(schema: Schema): ActorRef[_] => AnyRef = { (t: ActorRef[_]) =>
      Serialization.serializedActorPath(t.toClassic)
    }
  }

  implicit def SchemaForSourceRef[T]: SchemaFor[SourceRef[T]] = new SchemaFor[SourceRef[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit def EncoderForSourceRef[T](implicit eas: ExtendedActorSystem): Encoder[SourceRef[T]] =
    new Encoder[SourceRef[T]] {
      override def encode(schema: Schema): SourceRef[T] => AnyRef =
        (ref: SourceRef[T]) => StreamRefResolver.get(eas).toSerializationFormat(ref)

    }
  implicit def DecoderForSourceRef[T](implicit eas: ExtendedActorSystem): Decoder[SourceRef[T]] =
    new Decoder[SourceRef[T]] {
      override def decode(schema: Schema): Any => SourceRef[T] =
        (value: Any) => StreamRefResolver.get(eas).resolveSourceRef(value.toString)
    }

  implicit def SchemaForSinkRef[T]: SchemaFor[SinkRef[T]] = new SchemaFor[SinkRef[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)

  implicit def EncoderForSinkRef[T](implicit eas: ExtendedActorSystem): Encoder[SinkRef[T]] =
    new Encoder[SinkRef[T]] {
      override def encode(schema: Schema): SinkRef[T] => AnyRef =
        (ref: SinkRef[T]) => StreamRefResolver.get(eas).toSerializationFormat(ref)
    }
  implicit def DecoderForSinkRef[T](implicit eas: ExtendedActorSystem): Decoder[SinkRef[T]] =
    new Decoder[SinkRef[T]] {
      override def decode(schema: Schema): Any => SinkRef[T] =
        (value: Any) => StreamRefResolver.get(eas).resolveSinkRef(value.toString)
    }
}
