package com.radix.shared.persistence.test

import akka.persistence.CapabilityFlag
import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory
import io.github.embeddedkafka.schemaregistry.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.{AnyFlatSpec, AnyFlatSpecLike}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaSnapshotStoreTest
    extends SnapshotStoreSpec(
      ConfigFactory.load
        .withValue("pekko.remote.artery.canonical.port", ConfigFactory.parseString("{port: 2555}").getValue("port"))
    )
    with EmbeddedKafka
    with Matchers {

  override def supportsSerialization: CapabilityFlag = CapabilityFlag.off()

  override def beforeAll(): Unit = {
    implicit val config: EmbeddedKafkaConfig =
      EmbeddedKafkaConfig(
//        customSchemaRegistryProperties = Map("schema.compatibility.level"-> "full"),
      )
    EmbeddedKafka.start()
    super.beforeAll()
  }

//  it should "start successfully"  {
//
//  }

  override def afterAll(): Unit = {
    // both of these cause the test to fail for no reason

//    super.afterAll()
//    Await.result(system.terminate(), 10.seconds)

    EmbeddedKafka.stop()
  }
}
