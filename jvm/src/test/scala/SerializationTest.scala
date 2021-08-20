package com.radix.shared.persistence.test

import akka.NotUsed
import akka.actor.{ActorSystem, ExtendedActorSystem, ActorRef => UActorRef}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.testkit.{TestKit, TestProbe}
import akka.serialization.SerializationExtension
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.WordSpecLike
import org.scalatest.matchers.should.Matchers
import scala.util.{Failure, Success}

class ActorRefAvroSerializer[T](implicit currentSystem: ExtendedActorSystem) extends AvroSerializer[ActorRef[T]]
class TypedEchoSerializer(implicit currentSystem: ExtendedActorSystem) extends AvroSerializer[TypedEchoActor.Echo]
class UntypedEchoSerializer(implicit currentSystem: ExtendedActorSystem) extends AvroSerializer[UntypedEchoActor.Echo]

object TypedEchoActor {
  sealed trait Request
  final case class Echo(msg: String, replyTo: ActorRef[NotUsed]) extends Request

  def apply(): Behavior[Request] = {
    Behaviors.setup { _ =>
      Behaviors.receiveMessage { case Echo(_, _) => Behaviors.same }
    }
  }
}

object UntypedEchoActor {
  sealed trait Request
  final case class Echo(msg: String, replyTo: UActorRef) extends Request

  def apply(): Behavior[Request] = {
    Behaviors.setup { _ =>
      Behaviors.receiveMessage { case Echo(_, _) => Behaviors.same }
    }
  }
}

class TypedActorSerializationSpec
    extends ScalaTestWithActorTestKit("""
    |akka.actor.serializers.echo-serializer = "com.radix.shared.persistence.test.TypedEchoSerializer"
    |akka.actor.serialization-bindings."com.radix.shared.persistence.test.TypedEchoActor$Echo" = "echo-serializer"
    |""".stripMargin)
    with WordSpecLike {
  "Typed ActorRefs must (de)serialize correctly" in {
    val serialization = SerializationExtension(system.toClassic)
    val probe = testKit.createTestProbe[NotUsed]()
    val testMsg = TypedEchoActor.Echo("typed foo", probe.ref)
    Console.println(s"Message: $testMsg")
    serialization.findSerializerFor(testMsg) match {
      case avro: AvroSerializer[_] =>
        serialization.serialize(testMsg) match {
          case Success(serializedTestMsg) =>
            val identifier = avro.identifier
            val manifest = avro.manifest(testMsg)
            serialization.deserialize(serializedTestMsg, identifier, manifest) match {
              case Success(deserializedTestMsg) =>
                deserializedTestMsg should ===(testMsg)
              case Failure(exception) => fail(s"failed to deserialize: $exception")
            }

          case Failure(exception) => fail(s"failed to serialize: $exception")
        }
    }
  }
}

object UntypedActorSerializationSpec {
  val config: Config = ConfigFactory.parseString("""
      |akka.actor.serializers.echo-serializer = "com.radix.shared.persistence.test.UntypedEchoSerializer"
      |akka.actor.serialization-bindings."com.radix.shared.persistence.test.UntypedEchoActor$Echo" = "echo-serializer"
      |""".stripMargin)
}

class UntypedActorSerializationSpec
    extends TestKit(ActorSystem("UntypedActorSerializationSpec", UntypedActorSerializationSpec.config))
    with Matchers
    with WordSpecLike {
  "Untyped ActorRefs must (de)serialize correctly" in {
    val serialization = SerializationExtension(system)
    val probe = TestProbe()
    val testMsg = UntypedEchoActor.Echo("untyped foo", probe.ref)
    Console.println(s"Message: $testMsg")
    serialization.findSerializerFor(testMsg) match {
      case avro: AvroSerializer[_] =>
        serialization.serialize(testMsg) match {
          case Success(serializedTestMsg) =>
            val identifier = avro.identifier
            val manifest = avro.manifest(testMsg)
            serialization.deserialize(serializedTestMsg, identifier, manifest) match {
              case Success(deserializedTestMsg) =>
                deserializedTestMsg should ===(testMsg)
              case Failure(exception) => fail(s"failed to deserialize: $exception")
            }

          case Failure(exception) => fail(s"failed to serialize: $exception")
        }
    }
  }
}
