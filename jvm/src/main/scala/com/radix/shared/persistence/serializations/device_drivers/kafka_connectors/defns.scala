package com.radix.shared.persistence.serializations.device_drivers.kafka_connectors

import com.radix.shared.persistence.AvroSerializer

object defns {
  sealed trait HealthUpdate
  final case class ErrorOccurred(message: String) extends HealthUpdate
  final case class ConnectorActive() extends HealthUpdate
  final case class ConnectorDead(cod: String) extends HealthUpdate
  final case class ConnectorStopped() extends HealthUpdate

  final case class UpdateConnectorStatus()

  case class ErrorOccurredSerializer() extends AvroSerializer[ErrorOccurred]
  case class ConnectorActiveSerializer() extends AvroSerializer[ConnectorActive]
  case class ConnectorDeadSerializer() extends AvroSerializer[ConnectorDead]
  case class ConnectorStoppedSerializer() extends AvroSerializer[ConnectorStopped]
}