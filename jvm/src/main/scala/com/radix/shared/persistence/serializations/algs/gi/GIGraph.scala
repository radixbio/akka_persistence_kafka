package com.radix.shared.persistence.serializations.algs.gi

import com.radix.shared.persistence.AvroSerializer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}

import scala.reflect.ClassTag

object GIGraph {
  case class Edge[T: Encoder: Decoder: SchemaFor: ClassTag](from: T, to: T, weight: Option[Double])
  object Edge {
    implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag]: AvroSerializer[Edge[T]] = new AvroSerializer[Edge[T]]
  }
  case class Graph[T: Encoder: Decoder: SchemaFor: ClassTag](nodes: Array[T], edges: Array[Edge[T]])
  object Graph {
    implicit def avro[T: Encoder: Decoder: SchemaFor: ClassTag]: AvroSerializer[Graph[T]] = new AvroSerializer[Graph[T]]
  }
}
