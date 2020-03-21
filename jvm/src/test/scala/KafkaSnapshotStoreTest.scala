package com.radix.shared.persistence.test

import akka.persistence.CapabilityFlag
import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory
import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel
import net.manub.embeddedkafka.schemaregistry.{EmbeddedKafka, EmbeddedKafkaConfig}
import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaSnapshotStoreTest
  extends SnapshotStoreSpec(ConfigFactory.load.withValue("akka.remote.netty.tcp.port", ConfigFactory.parseString("{port: 2555}").getValue("port")))
    with EmbeddedKafka {

  override def supportsSerialization: CapabilityFlag = CapabilityFlag.off

  override def beforeAll(): Unit = {
    implicit val config: EmbeddedKafkaConfig =
      EmbeddedKafkaConfig(
        kafkaPort = 9092,
        zooKeeperPort = 2181,
        schemaRegistryPort = 8081,
        avroCompatibilityLevel = AvroCompatibilityLevel.FULL
      )
    EmbeddedKafka.start()

    super.beforeAll()
  }

  override def afterAll(): Unit = {
   // both of these cause the test to fail for no reason

//    super.afterAll()
//    Await.result(system.terminate(), 10.seconds)

    EmbeddedKafka.stop()
  }
}