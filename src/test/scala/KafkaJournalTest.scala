package com.radix.shared.persistence.test

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.{Config, ConfigFactory}
import io.github.embeddedkafka.schemaregistry.{EmbeddedKafka, EmbeddedKafkaConfig}
import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaJournalTest
    extends JournalSpec(
      ConfigFactory.load
        .withValue("akka.remote.artery.canonical.port", ConfigFactory.parseString("{port: 2554}").getValue("port"))
    )
    with EmbeddedKafka {

  override def supportsRejectingNonSerializableObjects: CapabilityFlag = CapabilityFlag.on()

  override def supportsSerialization: CapabilityFlag = CapabilityFlag.off()

  override def beforeAll(): Unit = {
    implicit val config: EmbeddedKafkaConfig =
      EmbeddedKafkaConfig(
        customSchemaRegistryProperties = Map("schema.compatibility.level" -> "full")
      )
    EmbeddedKafka.start()

    super.beforeAll()
  }

  override def afterAll(): Unit = {
    // both of these cause the test to fail for no reason
//  super.afterAll()
//    Await.result(system.terminate(), 10.seconds)

    EmbeddedKafka.stop()
  }
}
