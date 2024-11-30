package com.radix.shared.persistence.test

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.*
import akka.serialization.{SerializationExtension, SerializerWithStringManifest}
import akka.testkit.TestKit
import com.radix.shared.persistence.AvroSerializer
import org.scalatest.wordspec.AnyWordSpecLike
import com.radix.shared.persistence.test.RadixTestHoconConfig

import java.util.UUID
import scala.util.{Failure, Success}

abstract class SerializationTest extends ScalaTestWithActorTestKit(new RadixTestHoconConfig().config) with AnyWordSpecLike {
  def serializeAndDeserialize[T <: AnyRef](original: T): T = {
    val serialization = SerializationExtension(system.toClassic)
    serialization.findSerializerFor(original) match {
      case serz: SerializerWithStringManifest =>
        serialization.serialize(original) match {
          case Success(serializedObject) =>
            val identifier = serz.identifier
            val manifest = serz.manifest(original)
            serialization.deserialize(serializedObject, identifier, manifest) match {
              case Success(deserialized) =>
                deserialized.getClass should ===(original.getClass)
                deserialized.asInstanceOf[T]
              case Failure(exception) => fail(s"failed to deserialize: $exception")
            }

          case Failure(exception) =>
            fail(s"failed to serialize: $exception")
        }
      case other =>
        fail(s"unexpected serializer: $other")
    }
  }

  def serializeAndAssertEqual[T <: AnyRef](original: T)(assertEq: (T, T) => Unit = defaultAssertEq[T] _): Unit = {
    assertEq(serializeAndDeserialize(original), original)
  }

  def serializeManyAndAssertEqual[T <: AnyRef](
    originals: Iterable[T]
  )(assertEq: (T, T) => Unit = defaultAssertEq[T] _): Unit = {
    originals.foreach { original =>
      val actual = serializeAndDeserialize(original)
      assertEq(actual, original)
    }
  }

  private def defaultAssertEq[T](actual: T, expected: T): Unit = {
    actual should ===(expected)
  }

  def dummyActor: ActorRef[Any] = this.system.systemActorOf(Behaviors.ignore[Any], UUID.randomUUID.toString)
}
