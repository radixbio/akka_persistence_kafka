package com.radix.shared.persistence.serializations.utils.scheduler

import java.util.UUID

import akka.actor.ExtendedActorSystem
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8

import scala.collection.JavaConverters._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.utils.scheduler.SchedulerInputImplementation.ConsumerProducer
import com.radix.utils.scheduleruservice.SchedulerActorProtocol.{USchedulerRequest, USchedulerResponse}

object derivations {}

object Serializers {

  class USchedulerRequestPersist(implicit eas: ExtendedActorSystem) extends AvroSerializer[USchedulerRequest]

  class USchedulerResponsePersist extends AvroSerializer[USchedulerResponse]

}
