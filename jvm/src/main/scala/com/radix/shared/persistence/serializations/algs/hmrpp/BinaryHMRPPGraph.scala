package com.radix.shared.persistence.serializations.algs.hmrpp

import com.radix.shared.persistence.AvroSerializer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}

import scala.reflect.ClassTag

object BinaryHMRPPGraph {

  case class Edge[T: Encoder: Decoder: SchemaFor: ClassTag ](from: T, to: T, directed: Boolean, weight: Option[Double], layer: Option[Int])
  object Edge {
    implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag]: AvroSerializer[Edge[T]] = new AvroSerializer[Edge[T]]
  }
  case class Graph[T: Encoder: Decoder: SchemaFor: ClassTag](nodes: Array[T], edges: Array[Edge[T]])
  object Graph {
     implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag]: AvroSerializer[Graph[T]] = new AvroSerializer[Graph[T]]
  }
}
