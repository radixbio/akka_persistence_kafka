package com.radix.shared.persistence.test

import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueFactory}
import io.github.embeddedkafka.schemaregistry.EmbeddedKafkaConfig
import akka.actor.Address


class RadixTestHoconConfig(
  conf: => Config = ConfigFactory.load(),
  val extraSeeds: List[Address] = List.empty,
  val pekkoPort: Int = 2557,
  val kafkaBasePort: Int = 5489,
) {

  /**
   * Return the HOSTNAME property, defaulting to localhost
   */
  def getHostname: String =
    sys.props.getOrElse("HOSTNAME", sys.env.getOrElse("HOSTNAME", "localhost"))

  /**
   * Set any environment variables needed by the Akka actor system / journaling system. This will
   * be called right before the actor system is created
   */
  def configureEnvironment(): Unit = {
    val host = getHostname
    println(s"[TestKit] Configuring environment, host is $host")
    System.setProperty("HOSTNAME", host)
    System.setProperty("INTERNAL_HOSTNAME", host)
    System.setProperty("TCP_PEKKO_PORT", pekkoPort.toString)
    System.setProperty("INTERNAL_PORT", pekkoPort.toString)
    System.setProperty("KAFKA_BOOTSTRAP_SERVERS", s"PLAINTEXT://localhost:$kafkaBasePort")
    System.setProperty("AVRO_SCHEMA_REGISTRY_URL", s"http://localhost:${kafkaBasePort + 1}")
  }

  def applyConfMods(base: Config): Config = {
    println("[TestKit] Initializing overridden config")

    implicit def toConfVal(any: Any): ConfigValue = ConfigValueFactory.fromAnyRef(any)

    base
      .withValue("pekko.cluster.min-nr-of-members", 1)
      .withValue(
        "pekko.remote.artery.ssl.ssl-engine-provider",
        "akka.remote.artery.tcp.ConfigSSLEngineProvider",
      )
      .withValue("pekko.remote.artery.transport", "tcp")
      .withValue("pekko.cluster.downing-provider-class", "akka.cluster.sbr.SplitBrainResolverProvider")
      .withValue("pekko.cluster.typed.receptionist.write-consistency", "local")
      .withValue(
        "pekko.cluster.distributed-data.durable.keys",
        ConfigValueFactory.fromIterable(new java.util.ArrayList[String]),
      )
      .withValue("pekko.cluster.split-brain-resolver.static-quorum.quorum-size", 1)
  }

  lazy val config: Config = applyConfMods(conf)

  lazy val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    kafkaPort = kafkaBasePort,
    schemaRegistryPort = kafkaBasePort + 1,
    customSchemaRegistryProperties = Map("schema.compatibility.level" -> "full"),
//    avroCompatibilityLevel = AvroCompatibilityLevel.FULL
  )
}
