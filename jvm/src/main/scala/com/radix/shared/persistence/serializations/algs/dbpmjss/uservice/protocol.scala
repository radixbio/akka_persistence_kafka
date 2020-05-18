package com.radix.shared.persistence.serializations.algs.dbpmjss.uservice
import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.serializations.algs.dbpmjss.datatypes._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._

object protocol {
  final case class Solve(machine: Machines, jobs: Jobs, replyTo: ActorRef[Schedule])

  class DBPMJSSSolve(implicit as: ExtendedActorSystem) extends AvroSerializer[Solve]
}