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
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }
  implicit def EncoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Encoder[ServiceKey[T]] = new Encoder[ServiceKey[T]] {
    val ser = SerializationExtension(A)
    override def encode(t: ServiceKey[T]): AnyRef = {
      implicitly[Encoder[String]].encode(ser.findSerializerFor(t).toBinary(t).mkString)
    }
    override def schemaFor: SchemaFor[ServiceKey[T]] = SchemaForTypedServiceKey[T]
  }
  implicit def DecoderForTypedServiceKey[T](implicit A: ExtendedActorSystem): Decoder[ServiceKey[T]] = {
    new Decoder[ServiceKey[T]] {
      override def decode(value: Any): ServiceKey[T] = {
        ServiceKey(implicitly[Decoder[String]].decode(value)).asInstanceOf[ServiceKey[T]]
      }
      override def schemaFor: SchemaFor[ServiceKey[T]] = SchemaForTypedServiceKey[T]
    }
  }

  implicit def SchemaForAnyServiceKey(implicit A: ExtendedActorSystem): SchemaFor[ServiceKey[_]] = new SchemaFor[ServiceKey[_]] {
    override def schema: Schema = SchemaBuilder.builder.stringType()
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper

  }
  implicit def EncoderForAnyServiceKey(implicit A: ExtendedActorSystem): Encoder[ServiceKey[_]] = new Encoder[ServiceKey[_]] {
    val ser = SerializationExtension(A)
    override def encode(t: ServiceKey[_]): AnyRef = {
      implicitly[Encoder[String]].encode(ser.findSerializerFor(t).toBinary(t).mkString)
    }
    override def schemaFor: SchemaFor[ServiceKey[_]] = SchemaForAnyServiceKey
  }
  implicit def DecoderForAnyServiceKey(implicit A: ExtendedActorSystem): Decoder[ServiceKey[_]] = {
    new Decoder[ServiceKey[_]] {
      override def decode(value: Any): ServiceKey[_] = {
        ServiceKey(implicitly[Decoder[String]].decode(value))
      }
      override def schemaFor: SchemaFor[ServiceKey[_]] = SchemaForAnyServiceKey
    }
  }


}
