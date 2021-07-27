package com.radix.shared.persistence.serializations.algs.gi.uservice

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.algs.gi.GIGraph._
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import com.radix.shared.persistence.ActorRefSerializer._

import scala.reflect.ClassTag

object protocol {
  sealed trait IsomorphismRequest
  case class IsomorphismFind[T: Encoder: Decoder: SchemaFor: ClassTag](
    largerGraph: Graph[T],
    subGraph: Graph[T],
    replyTo: ActorRef[IsomorphismResponse[T]]
  ) extends IsomorphismRequest {
    val enc = implicitly[Encoder[T]]
    val dec = implicitly[Decoder[T]]
    val sche = implicitly[SchemaFor[T]]
  }
  object IsomorphismFind {
    implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag](implicit
      A: ExtendedActorSystem
    ): AvroSerializer[IsomorphismFind[T]] = new AvroSerializer[IsomorphismFind[T]]
  }
  //NOTE: the list here is since avro can't have a map[T, T], just a map[String, T]
  case class IsomorphismResponse[T: Encoder: Decoder: SchemaFor: ClassTag](response: Option[List[(T, T)]])
  object IsomorphismResponse {
    implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag]: AvroSerializer[IsomorphismResponse[T]] =
      new AvroSerializer[IsomorphismResponse[T]]
  }
}
