package com.radix.shared.persistence.serializations.algs.hmrpp

import com.radix.shared.persistence.AvroSerializer
import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}

import scala.reflect.ClassTag

object BinaryHMRPPGraph {

  case class Edge[T: Encoder: Decoder: SchemaFor: ClassTag: Ordering](
    from: T,
    to: T,
    directed: Boolean,
    weight: Option[Double],
    layer: Option[Int]
  )

  case class Graph[T: Encoder: Decoder: SchemaFor: ClassTag: Ordering](nodes: Array[T], edges: Array[Edge[T]])

}
