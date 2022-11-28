package com.radix.shared.persistence

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

class KafkaConfig(cfg: Config) {
  val bootstrapServers = cfg.getString("kafka.bootstrap.servers")
  val producerConfig: Config = cfg.getConfig("kafka.producer")
  val consumerConfig: Config = cfg.getConfig("kafka.consumer")
  val streamParallelism: Int = cfg.getInt("stream-parallelism")
  val avroConfig = AvroConfig(cfg.getConfig("avro")).asJava
}
