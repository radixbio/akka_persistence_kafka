package com.radix.shared.persistence.test

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorPath, ActorSystem, Identify, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.PersistentActor
import com.radix.shared.persistence.AvroSerializer

case class Foo(x: Int)
case class Bar(y: Boolean)

class FooSerializer extends AvroSerializer[Foo]
class BarSerializer extends AvroSerializer[Bar]

class IDActor extends Actor with ActorLogging {
  def receive: PartialFunction[Any, Unit] = {
    case "start" =>
      log.info("Current Actors in system:")
      self ! ActorPath.fromString("akka://radix")

    case path: ActorPath =>
      context.actorSelection(path / "*") ! Identify(())

    case ActorIdentity(_, Some(ref)) =>
      log.info(ref.toString())
      self ! ref.path
  }
}

class SampleActor extends PersistentActor {
  val log: LoggingAdapter = Logging(context.system, this)
  val persistenceId = "blah"

  val receiveCommand: Receive = {
    case msg: Foo =>
      persist(msg) { _ => }
    case msg: Bar =>
      persist(msg) { _ => }
  }
  //saveSnapshot(msg ++ "BLAH")*/

  def receiveRecover: PartialFunction[Any, Unit] = {
    case msg =>
      log.info(msg.toString)
  }
}

object Test extends App {
  val system = ActorSystem("radix")
  val samplerRef = system.actorOf(Props[SampleActor](), name = "sampler")
  val idActorRef = system.actorOf(Props[IDActor](), name = "id-actor")
  0 to 1 foreach { n =>
    samplerRef ! Foo(n)
    samplerRef ! Bar(n % 2 == 0)
  }
  //idActorRef ! "start"
}
