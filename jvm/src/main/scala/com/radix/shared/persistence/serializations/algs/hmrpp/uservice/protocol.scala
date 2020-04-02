package com.radix.shared.persistence.serializations.algs.hmrpp.uservice

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.cluster.Cluster
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.algs.hmrpp.BinaryHMRPPGraph.Graph
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}

import scala.reflect.ClassTag

object protocol {
  sealed trait HMRPPRequest
  case class HMRPPUnweighted[T: Encoder: Decoder: SchemaFor: ClassTag](graph: Graph[T],
                                                                       replyTo: ActorRef[HMRPPResponse[T]])
      extends HMRPPRequest {
    val ser = implicitly[Encoder[T]]
    val dec = implicitly[Decoder[T]]
    val sche = implicitly[SchemaFor[T]]
  }
  object HMRPPUnweighted {
    implicit def avro[T: SchemaFor: ClassTag: Decoder: Encoder](
        implicit A: ExtendedActorSystem): AvroSerializer[HMRPPUnweighted[T]] = new AvroSerializer[HMRPPUnweighted[T]]
  }
  case class SingleHMRPPWalkUnweighted[T: Encoder: Decoder: SchemaFor: ClassTag](graph: Graph[T],
                                                                                 replyTo: ActorRef[HMRPPResponse[T]])
      extends HMRPPRequest {
    val ser = implicitly[Encoder[T]]
    val dec = implicitly[Decoder[T]]
    val sche = implicitly[SchemaFor[T]]
  }
  object SingleHMRPPWalkUnweighted {
    implicit def avro[T: SchemaFor: ClassTag: Decoder: Encoder](
        implicit A: ExtendedActorSystem): AvroSerializer[SingleHMRPPWalkUnweighted[T]] =
      new AvroSerializer[SingleHMRPPWalkUnweighted[T]]
  }
  // The above are maybe redundant, but there exists a test done using  non problem graph, and instead being converted automatically
  // so we offer support here.

  case class HMRPPProblem[T: Encoder: Decoder: SchemaFor: ClassTag](graph: Graph[T],
                                                                    replyTo: ActorRef[HMRPPResponse[T]])
      extends HMRPPRequest {
        val ser = implicitly[Encoder[T]]
    val dec = implicitly[Decoder[T]]
    val sche = implicitly[SchemaFor[T]]
  }
  object HMRPPProblem {
    implicit def avro[T: SchemaFor: ClassTag: Decoder: Encoder](
        implicit A: ExtendedActorSystem): AvroSerializer[HMRPPProblem[T]] = new AvroSerializer[HMRPPProblem[T]]
  }
  case class SingleHMRPPWalkProblem[T: Encoder: Decoder: SchemaFor: ClassTag](graph: Graph[T],
                                                                              replyTo: ActorRef[HMRPPResponse[T]])
      extends HMRPPRequest {
            val ser = implicitly[Encoder[T]]
    val dec = implicitly[Decoder[T]]
    val sche = implicitly[SchemaFor[T]]
  }
  object SingleHMRPPWalkProblem {
    implicit def avro[T: SchemaFor: ClassTag: Decoder: Encoder](
        implicit A: ExtendedActorSystem): AvroSerializer[SingleHMRPPWalkProblem[T]] =
      new AvroSerializer[SingleHMRPPWalkProblem[T]]
  }

  case class HMRPPResponse[T: SchemaFor: ClassTag: Decoder: Encoder](stuff: Either[Int, List[(Int, List[(T, T)])]])
  object HMRPPResponse {
    implicit def avro[T: SchemaFor: ClassTag: Decoder: Encoder]: AvroSerializer[HMRPPResponse[T]] =
      new AvroSerializer[HMRPPResponse[T]]
  }
}
