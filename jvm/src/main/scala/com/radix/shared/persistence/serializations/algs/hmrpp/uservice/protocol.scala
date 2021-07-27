package com.radix.shared.persistence.serializations.algs.hmrpp.uservice

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import akka.cluster.Cluster
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.serializations.algs.hmrpp.BinaryHMRPPGraph.{Edge, Graph}
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import shapeless.the

import scala.reflect.ClassTag

object protocol {

  case class Vertex(id: Int)
  object Vertex {
    implicit object VertexOrdering extends Ordering[Vertex] {
      override def compare(x: Vertex, y: Vertex): Int = x.id.compare(y.id)
    }
  }

  sealed trait HMRPPRequest
  case class HMRPPUnweightedInt(
    graph: Graph[Int],
    replyTo: ActorRef[HMRPPResponse[Int]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Int]]
    val dec = implicitly[Decoder[Int]]
    val sche = implicitly[SchemaFor[Int]]
  }

  case class HMRPPUnweightedVertex(
    graph: Graph[Vertex],
    replyTo: ActorRef[HMRPPResponse[Vertex]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Vertex]]
    val dec = implicitly[Decoder[Vertex]]
    val sche = implicitly[SchemaFor[Vertex]]
  }

  case class SingleHMRPPWalkUnweightedInt(
    graph: Graph[Int],
    replyTo: ActorRef[HMRPPResponse[Int]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Int]]
    val dec = implicitly[Decoder[Int]]
    val sche = implicitly[SchemaFor[Int]]
  }
  case class SingleHMRPPWalkUnweightedVertex(
    graph: Graph[Vertex],
    replyTo: ActorRef[HMRPPResponse[Vertex]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Vertex]]
    val dec = implicitly[Decoder[Vertex]]
    val sche = implicitly[SchemaFor[Vertex]]
  }
  // The above are maybe redundant, but there exists a test done using  non problem graph, and instead being converted automatically
  // so we offer support here.

  case class HMRPPProblemInt(
    graph: Graph[Int],
    replyTo: ActorRef[HMRPPResponse[Int]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Int]]
    val dec = implicitly[Decoder[Int]]
    val sche = implicitly[SchemaFor[Int]]
  }

  case class HMRPPProblemVertex(
    graph: Graph[Vertex],
    replyTo: ActorRef[HMRPPResponse[Vertex]]
  ) extends HMRPPRequest {
    val ser = implicitly[Encoder[Vertex]]
    val dec = implicitly[Decoder[Vertex]]
    val sche = implicitly[SchemaFor[Vertex]]
  }

  case class SingleHMRPPWalkProblemInt(
    graph: Graph[Int],
    replyTo: ActorRef[HMRPPResponse[Int]]
  ) extends HMRPPRequest {
    val ser = the[Encoder[Int]]
    val dec = the[Decoder[Int]]
    val sche = the[SchemaFor[Int]]
  }

  case class SingleHMRPPWalkProblemVertex(
    graph: Graph[Vertex],
    replyTo: ActorRef[HMRPPResponse[Vertex]]
  ) extends HMRPPRequest {
    val ser = the[Encoder[Vertex]]
    val dec = the[Decoder[Vertex]]
    val sche = the[SchemaFor[Vertex]]
  }

  sealed trait HMRPPResponse[T] {
    val stuff: Either[Int, List[(Int, List[(T, T)])]]
  }

  case class HMRPPResponseInt(stuff: Either[Int, List[(Int, List[(Int, Int)])]]) extends HMRPPResponse[Int]
  case class HMRPPResponseVertex(stuff: Either[Int, List[(Int, List[(Vertex, Vertex)])]]) extends HMRPPResponse[Vertex]

  class HMRPPResponseIntSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPResponseInt]
  class HMRPPResponseVertexSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPResponseVertex]

  class EdgeSerializer[T: Encoder: Decoder: SchemaFor: ClassTag: Ordering] extends AvroSerializer[Edge[T]]
  class GraphSerializer[T: Encoder: Decoder: SchemaFor: ClassTag: Ordering] extends AvroSerializer[Graph[T]]

  class HMRPPUnweightedIntSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPUnweightedInt]
  class HMRPPUnweightedVertexSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPUnweightedVertex]

  class SingleHMRPPWalkUnweightedIntSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[SingleHMRPPWalkUnweightedInt]
  class SingleHMRPPWalkUnweightedVertexSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[SingleHMRPPWalkUnweightedVertex]

  class HMRPPProblemIntSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPProblemInt]
  class HMRPPProblemVertexSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HMRPPProblemVertex]

  class SingleHMRPPWalkProblemIntSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[SingleHMRPPWalkProblemInt]
  class SingleHMRPPWalkProblemVertexSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[SingleHMRPPWalkProblemVertex]
}
