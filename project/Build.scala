package com.radix.shared.persistence

import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

object Versions {
  val scala          = "2.12.9"
  val akka           = "2.5.25"
  val kafka          = "2.3.0"
  val avroSerializer = "5.3.0"
}

object Dependencies {
  import Versions._
  val commonSettings = Def.settings(
    organization := "com.radix",
    scalaVersion := scala,
    name := "persistence",
    version := "0.0.1-SNAPSHOT",
    scalacOptions += "-Ypartial-unification",
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "io.confluent" at "https://packages.confluent.io/maven/",
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )

  val jvmLibraryDependencies = Def.settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"       %% "akka-actor"                     % Versions.akka,
      "com.typesafe.akka"       %% "akka-persistence"               % Versions.akka,
      "com.typesafe.akka"       %% "akka-stream"                    % Versions.akka,
      "com.typesafe.akka"       %% "akka-stream-kafka"              % "1.0.5",
      "com.typesafe.akka"       %% "akka-slf4j"                     % Versions.akka,
      "org.apache.kafka"        %% "kafka"                          % Versions.kafka,
      "org.apache.kafka"        %  "kafka-streams"                  % Versions.kafka,
      "org.apache.kafka"        %% "kafka-streams-scala"            % Versions.kafka,
      "com.typesafe.akka"       %% "akka-cluster-typed"             % Versions.akka,
      "com.typesafe.akka"       %% "akka-persistence-tck"           % Versions.akka,
      "com.typesafe.akka"       %% "akka-persistence-query"         % Versions.akka,
      "io.confluent"            %  "kafka-avro-serializer"          % Versions.avroSerializer,
      "org.typelevel"           %% "cats-core"                      % "2.0.0",
      "org.scalaz"              %% "scalaz-core"                    % "7.2.28",
      "com.sksamuel.avro4s"     %% "avro4s-core"                    % "3.0.1",
       compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik"         % "silencer-lib"                   % "1.4.4" % Provided cross CrossVersion.full,
      "io.github.embeddedkafka" %% "embedded-kafka-schema-registry" % "5.3.0" % "test"
    )
  )

  val persistence =
    crossProject(JVMPlatform).withoutSuffixFor(JVMPlatform).settings(commonSettings).jvmSettings(jvmLibraryDependencies)
}
