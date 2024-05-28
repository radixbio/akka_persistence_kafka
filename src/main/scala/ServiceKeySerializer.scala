package com.radix.shared.persistence

import java.nio.charset.StandardCharsets

import akka.actor.ExtendedActorSystem
import akka.actor.typed.receptionist.ServiceKey
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import akka.serialization.SerializationExtension

import scala.reflect.ClassTag
object ServiceKeySerializer {
  implicit def SchemaForTypedServiceKey[T]: SchemaFor[ServiceKey[T]] = new SchemaFor[ServiceKey[T]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
  }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)
  implicit def EncoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Encoder[ServiceKey[T]] =
    new Encoder[ServiceKey[T]] {
      lazy val ser = SerializationExtension.get(A)
      override def encode(schema: Schema): ServiceKey[T] => Any = { // (t: ServiceKey[T]): Any = {
        (t: ServiceKey[T]) =>
          implicitly[Encoder[String]].encode(SchemaBuilder.builder.stringType())(
            ser.findSerializerFor(t).toBinary(t).mkString
          )
      }
    }
  implicit def DecoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Decoder[ServiceKey[T]] = {
    new Decoder[ServiceKey[T]] {
      override def decode(schema: Schema): Any => ServiceKey[T] = { (value: Any) =>
        ServiceKey[String](implicitly[Decoder[String]].decode(SchemaBuilder.builder.stringType())(value))
          .asInstanceOf[ServiceKey[T]]
      }
    }
  }

  implicit def SchemaForAnyServiceKey(implicit A: ExtendedActorSystem): SchemaFor[ServiceKey[_]] =
    new SchemaFor[ServiceKey[_]] {
      override def schema: Schema = SchemaBuilder.builder.stringType()

    }.withFieldMapper(com.sksamuel.avro4s.DefaultFieldMapper)
  implicit def EncoderForAnyServiceKey(implicit A: ExtendedActorSystem): Encoder[ServiceKey[_]] =
    new Encoder[ServiceKey[_]] {
      lazy val ser = SerializationExtension.get(A)
      override def encode(schema: Schema): ServiceKey[_] => Any = { (t: ServiceKey[_]) =>
        implicitly[Encoder[String]].encode(SchemaBuilder.builder.stringType())(
          ser.findSerializerFor(t).toBinary(t).mkString
        )
      }
    }
  implicit def DecoderForAnyServiceKey(implicit A: ExtendedActorSystem): Decoder[ServiceKey[_]] = {
    new Decoder[ServiceKey[_]] {
      override def decode(schema: Schema): Any => ServiceKey[_] = { (value: Any) =>
        ServiceKey[String](implicitly[Decoder[String]].decode(SchemaBuilder.builder.stringType())(value))
          .asInstanceOf[ServiceKey[Any]]
      }
    }
  }

}
