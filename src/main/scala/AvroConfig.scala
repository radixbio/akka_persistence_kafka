package com.radix.shared.persistence

import com.typesafe.config.Config
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig

object AvroConfig {
  def apply(cfg: Config): Map[String, Any] = {
    Map[String, Any](
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> cfg.getString("schema.registry.url"),
      AbstractKafkaAvroSerDeConfig.KEY_SUBJECT_NAME_STRATEGY -> cfg.getString("key.subject.name.strategy"),
      AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> cfg.getString("value.subject.name.strategy"),
    )
  }
}
