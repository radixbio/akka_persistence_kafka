package com.radix.shared.persistence.serializations.device_drivers.watlow96_thermal

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import squants.Temperature

import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._

object defns {
  sealed trait Watlow96Request

//  case class SetManual(manual: Boolean)
//  case class

  case class Reinitialize(replyTo: Option[ActorRef[SetResponse]]) extends Watlow96Request
  case class SetPower(power: Int, replyTo: Option[ActorRef[SetResponse]]) extends Watlow96Request
  case class SetTemperature(temperature: Temperature, replyTo: Option[ActorRef[SetResponse]]) extends Watlow96Request
  case class QueryPowerLevel(replyTo: Option[ActorRef[QueryResponse]]) extends Watlow96Request

  sealed trait Watlow96Response

  sealed trait SetResponse extends Watlow96Response
  sealed trait QueryResponse extends Watlow96Response
  case class InitializedDevice() extends SetResponse with Watlow96Event
  case class ChangedSetpoint(newValue: Int) extends SetResponse with Watlow96Event
  case class GotPowerLevel(power: Int) extends QueryResponse with Watlow96Event

  sealed trait Watlow96CommonResponse extends Watlow96Response with SetResponse with QueryResponse
  case class ErrorOccurred(cause: String) extends Watlow96CommonResponse with Watlow96Event
  case class BadRequest(reason: String) extends Watlow96CommonResponse

  sealed trait Watlow96Event

  class ReinitializeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Reinitialize]
  class SetPowerSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetPower]
  class SetTemperatureSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetTemperature]
  class QueryPowerLevelSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[QueryPowerLevel]
  class InitializedDeviceSerializer extends AvroSerializer[InitializedDevice]
  class ChangedSetpointSerializer extends AvroSerializer[ChangedSetpoint]
  class GotPowerLevelSerializer extends AvroSerializer[GotPowerLevel]
  class ErrorOccurredSerializer extends AvroSerializer[ErrorOccurred]
  class BadRequestSerializer extends AvroSerializer[BadRequest]
}
