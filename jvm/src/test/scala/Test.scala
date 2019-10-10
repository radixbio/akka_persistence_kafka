package com.radix.shared.persistence.test

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorPath, ActorSystem, Identify, Props}
import akka.event.Logging
import akka.persistence.PersistentActor
import com.radix.shared.persistence.AvroSerializer

case class Foo(x: Int)

class FooSerializer extends AvroSerializer[Foo]

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
  val log = Logging(context.system, this)
  val persistenceId = "blah"

  val receiveCommand: Receive = {
    case msg: Foo => {
      persist(msg) { _ => }
    }
  }
    //saveSnapshot(msg ++ "BLAH")*/

  def receiveRecover: PartialFunction[Any, Unit] = {
    case "foo" => log.info("foo!")
    case "bar" => log.info("bar!")
    case msg => log.error(msg.toString)
  }
}

object Test extends App {
  val system = ActorSystem("radix")
  val samplerRef = system.actorOf(Props[SampleActor], name = "sampler")
  val idActorRef = system.actorOf(Props[IDActor], name = "id-actor")
  /*0 to 1 foreach { n =>
    samplerRef ! Foo(n)
  }*/
  //idActorRef ! "start"
}