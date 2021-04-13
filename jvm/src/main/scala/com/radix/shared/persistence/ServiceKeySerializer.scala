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
    override def schema(fieldMapper: FieldMapper): Schema = SchemaBuilder.builder.stringType()
  }
  implicit def EncoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Encoder[ServiceKey[T]] = new Encoder[ServiceKey[T]] {
    val ser = SerializationExtension(A)
    override def encode(t: ServiceKey[T], schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      implicitly[Encoder[String]].encode(ser.findSerializerFor(t).toBinary(t).mkString, schema, fieldMapper)
    }
  }
  implicit def DecoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Decoder[ServiceKey[T]] = {
    new Decoder[ServiceKey[T]] {
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): ServiceKey[T] = {
        ServiceKey(implicitly[Decoder[String]].decode(value, schema, fieldMapper)).asInstanceOf[ServiceKey[T]]
      }
    }
  }

  implicit def SchemaForAnyServiceKey(implicit A: ExtendedActorSystem): SchemaFor[ServiceKey[_]] = new SchemaFor[ServiceKey[_]] {
    override def schema(fieldMapper: FieldMapper): Schema = SchemaBuilder.builder.stringType()
  }
  implicit def EncoderForAnyServiceKey(implicit A: ExtendedActorSystem): Encoder[ServiceKey[_]] = new Encoder[ServiceKey[_]] {
    val ser = SerializationExtension(A)
    override def encode(t: ServiceKey[_], schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      implicitly[Encoder[String]].encode(ser.findSerializerFor(t).toBinary(t).mkString, schema, fieldMapper)
    }
  }
  implicit def DecoderForAnyServiceKey(implicit A: ExtendedActorSystem): Decoder[ServiceKey[_]] = {
    new Decoder[ServiceKey[_]] {
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): ServiceKey[_] = {
        ServiceKey(implicitly[Decoder[String]].decode(value, schema, fieldMapper))
      }
    }
  }


}
