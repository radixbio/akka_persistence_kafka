package com.radix.shared.persistence.serializations.utils.rainbow

import java.util.UUID

import akka.actor.ExtendedActorSystem
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import com.radix.shared.persistence.serializations.squants.schemas._

import scala.collection.JavaConverters._
import java.util

import akka.persistence.SnapshotMetadata
import com.radix.rainbow.LabMeta
import com.radix.rainbow.URainbow.{CommandMetadata, RainbowCommandAndReverse, RainbowHistoricalResponse, RainbowHistoricalResponseWithParents, RainbowModifyCommand, URainbowEvent, UpdateRainbow, ValidReverse}
import com.radix.rainbow.URainbow.UTypes.{ID, Metadata, RainbowMetaSerialized}
import com.radix.shared.persistence.AvroSerializer
import com.radix.utils.rainbowuservice.RainbowActorProtocol
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.utils.rainbowuservice.RainbowActorProtocol.{RainbowHistoryResponse, RainbowHistoryWithParentResponse, URainbowCommand, URainbowResponse} //this is necessary

object derivations {
  implicit val fieldMapper: FieldMapper = DefaultFieldMapper

  implicit def mapSchemaForUUID[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[UUID, V]] = {
    new SchemaFor[Map[UUID, V]] {
      override def schema: Schema =
        SchemaBuilder.map().values(schemaFor.schema)
      override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
    }
  }

  implicit def mapDecoderUUID[T](implicit valueDecoder: Decoder[T], schemaForImp: SchemaFor[T]): Decoder[Map[UUID, T]] =
    new Decoder[Map[UUID, T]] {

      override def decode(value: Any): Map[UUID, T] =
        value match {
          case map: java.util.Map[_, _] =>
            map.asScala.toMap.map {
              case (k, v) =>
                UUID.fromString(
                  implicitly[Decoder[String]]
                    .decode(k)
                ) -> valueDecoder.decode(v)
            }
          case other => sys.error("Unsupported map " + other)
        }
      override def schemaFor: SchemaFor[Map[UUID, T]] = mapSchemaForUUID[T]
    }

  implicit def mapEncoderUUID[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[UUID, V]] =
    new Encoder[Map[UUID, V]] {

      override def encode(map: Map[UUID, V]): java.util.Map[String, AnyRef] = {
        require(schema != null)
        val java = new util.HashMap[String, AnyRef]
        map.foreach {
          case (k, v) =>
            java.put(k.toString, encoder.encode(v))
        }
        java
      }

      override def schemaFor: SchemaFor[Map[UUID, V]] = mapSchemaForUUID[V]
    }

  implicit def mapSchemaForInt[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[Int, V]] = {
    new SchemaFor[Map[Int, V]] {
      override def schema: Schema =
        SchemaBuilder.map().values(schemaFor.schema)
      override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
    }
  }

  implicit def mapDecoderInt[T](implicit valueDecoder: Decoder[T], schemaForImp: SchemaFor[T]): Decoder[Map[Int, T]] =
    new Decoder[Map[Int, T]] {

      override def decode(value: Any): Map[Int, T] =
        value match {
          case map: java.util.Map[_, _] =>
            map.asScala.toMap.map {
              case (k, v) =>
                (
                  implicitly[Decoder[String]]
                    .decode(k)
                  )
                  .toInt -> valueDecoder.decode(v)
            }
          case other => sys.error("Unsupported map " + other)
        }
      override def schemaFor: SchemaFor[Map[Int, T]] = mapSchemaForInt[T]
    }

  implicit def mapEncoderInt[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[Int, V]] =
    new Encoder[Map[Int, V]] {

      override def encode(map: Map[Int, V]): java.util.Map[String, AnyRef] = {
        require(schema != null)
        val java = new util.HashMap[String, AnyRef]
        map.foreach {
          case (k, v) =>
            java.put(k.toString, encoder.encode(v))
        }
        java
      }
      override def schemaFor: SchemaFor[Map[Int, V]] = mapSchemaForInt[V]
    }
}

//From here down, all is horrible. Each of this schemas and encoders are so large
//that if they are in the same object, they go over the jvm limit of 64kb per file
//they are separated out of necessity
object fixers1 {
  import derivations._
  val schema: SchemaFor[LabMeta[Metadata]] = SchemaFor[LabMeta[Metadata]] //workaround to get things working
  val enc: Encoder[LabMeta[Metadata]] = Encoder[LabMeta[Metadata]] //workaround to get things working
  val dec: Decoder[LabMeta[Metadata]] = Decoder[LabMeta[Metadata]] //workaround to get things working
}
object fixers2 {
  import implicitFixers1._
  import derivations._
  val schema: SchemaFor[RainbowMetaSerialized] = SchemaFor[RainbowMetaSerialized] //workaround to get things working
  val enc: Encoder[RainbowMetaSerialized] = Encoder[RainbowMetaSerialized] //workaround to get things working
  val dec: Decoder[RainbowMetaSerialized] = Decoder[RainbowMetaSerialized] //workaround to get things working
}
object fixers2_0 {
  import implicitFixers1._
  import implicitFixers2._
  import derivations._
  val enc: Encoder[RainbowModifyCommand] = Encoder[RainbowModifyCommand] //workaround to get api actor working
  val dec: Decoder[RainbowModifyCommand] = Decoder[RainbowModifyCommand] //workaround to get api actor working
  val schema: SchemaFor[RainbowModifyCommand] = SchemaFor[RainbowModifyCommand] //workaround to get api actor working
}
object fixers2_00 {
  import implicitFixers1._
  import implicitFixers2._
  import derivations._
  val enc: Encoder[ValidReverse] = Encoder[ValidReverse] //workaround to get api actor working
  val dec: Decoder[ValidReverse] = Decoder[ValidReverse] //workaround to get api actor working
  val schema: SchemaFor[ValidReverse] = SchemaFor[ValidReverse] //workaround to get api actor working
}
object fixers4 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import derivations._
  val enc: Encoder[RainbowCommandAndReverse] = Encoder[RainbowCommandAndReverse] //workaround to get api actor working
  val dec: Decoder[RainbowCommandAndReverse] = Decoder[RainbowCommandAndReverse] //workaround to get api actor working
  val schema
    : SchemaFor[RainbowCommandAndReverse] = SchemaFor[RainbowCommandAndReverse] //workaround to get api actor working
}
object fixers5 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers4._
  import derivations._
  val enc: Encoder[UpdateRainbow] = Encoder[UpdateRainbow] //workaround to get api actor working
  val dec: Decoder[UpdateRainbow] = Decoder[UpdateRainbow] //workaround to get api actor working
  val schema: SchemaFor[UpdateRainbow] = SchemaFor[UpdateRainbow] //workaround to get api actor working
}
object fixers6 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers4._
  import implicitFixers5._
  import derivations._
  val enc: Encoder[URainbowEvent] = Encoder[URainbowEvent] //workaround to get api actor working
  val dec: Decoder[URainbowEvent] = Decoder[URainbowEvent] //workaround to get api actor working
  val schema: SchemaFor[URainbowEvent] = SchemaFor[URainbowEvent] //workaround to get api actor working
}

object fixers7_11 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val enc1
    : Encoder[RainbowHistoricalResponse] = Encoder[RainbowHistoricalResponse] //workaround to get api actor working
}

object fixers7_12 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val dec1
    : Decoder[RainbowHistoricalResponse] = Decoder[RainbowHistoricalResponse] //workaround to get api actor working
}

object fixers7_13 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val schema1
    : SchemaFor[RainbowHistoricalResponse] = SchemaFor[RainbowHistoricalResponse] //workaround to get api actor working
}

object fixers7_21 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val enc2: Encoder[RainbowHistoricalResponseWithParents] = Encoder[RainbowHistoricalResponseWithParents] //workaround to get api actor working

}
object fixers7_22 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val dec2: Decoder[RainbowHistoricalResponseWithParents] = Decoder[RainbowHistoricalResponseWithParents] //workaround to get api actor working

}
object fixers7_23 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import derivations._

  val schema2: SchemaFor[RainbowHistoricalResponseWithParents] = SchemaFor[RainbowHistoricalResponseWithParents] //workaround to get api actor working
}

object fixers8_11 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._

  val enc1: Encoder[RainbowHistoryResponse] = Encoder[RainbowHistoryResponse] //workaround to get api actor working
}
object fixers8_12 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._

  val dec1: Decoder[RainbowHistoryResponse] = Decoder[RainbowHistoryResponse] //workaround to get api actor working

}
object fixers8_13 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._

  val schema1
    : SchemaFor[RainbowHistoryResponse] = SchemaFor[RainbowHistoryResponse] //workaround to get api actor working

}

object fixers8_21 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._
  val enc2: Encoder[RainbowHistoryWithParentResponse] = Encoder[RainbowHistoryWithParentResponse] //workaround to get api actor working
}
object fixers8_22 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._
  val dec2: Decoder[RainbowHistoryWithParentResponse] = Decoder[RainbowHistoryWithParentResponse] //workaround to get api actor working

}
object fixers8_23 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import derivations._
  val schema2: SchemaFor[RainbowHistoryWithParentResponse] = SchemaFor[RainbowHistoryWithParentResponse] //workaround to get api actor working

}

object fixers2_1 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import derivations._
  val enc
    : Encoder[URainbowResponse] = Encoder[RainbowActorProtocol.URainbowResponse] //workaround to get api actor working
}
object fixers2_2 {
  import implicitFixers1._
  import implicitFixers2._
  import derivations._
  import implicitFixers2_0._
  val dec
    : Decoder[URainbowResponse] = Decoder[RainbowActorProtocol.URainbowResponse] //workaround to get api actor working
}

object fixers2_3 {
  import implicitFixers2_0._
  import implicitFixers1._
  import implicitFixers2._
  import derivations._
  val enc: Encoder[URainbowCommand] = Encoder[URainbowCommand] //workaround to get api actor working
//  val dec: Decoder[URainbowCommand] = Decoder[URainbowCommand] //workaround to get api actor working
}

object fixers3 {
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import derivations._
  val schema1: SchemaFor[URainbowResponse] = SchemaFor[RainbowActorProtocol.URainbowResponse] //workaround to get api actor working
  val schema2
    : SchemaFor[URainbowCommand] = SchemaFor[RainbowActorProtocol.URainbowCommand] //workaround to get api actor working
}

object implicitFixers1 {
  implicit val schLabMeta = fixers1.schema
  implicit val encLabMeta = fixers1.enc
  implicit val decLabMeta = fixers1.dec
}
object implicitFixers2 {
  implicit val schRainbowMeta = fixers2.schema
  implicit val encRainbowMeta = fixers2.enc
  implicit val decRainbowMeta = fixers2.dec
}
object implicitFixers2_0 {
  implicit val encMod = fixers2_0.enc
  implicit val decMod = fixers2_0.dec
  implicit val schMod = fixers2_0.schema

  implicit val encReverse = fixers2_00.enc
  implicit val decReverse = fixers2_00.dec
  implicit val schReverse = fixers2_00.schema
}
object implicitFixers2_1 {
  implicit val encResp = fixers2_1.enc
  implicit val decResp = fixers2_2.dec
}
object implicitFixers2_3 {
  implicit val encCommand = fixers2_3.enc

}
object implicitFixers3 {
  implicit val schResponse = fixers3.schema1
  implicit val schCommand = fixers3.schema2
}
object implicitFixers4 {
  implicit val encCommRev = fixers4.enc
  implicit val decCommRev = fixers4.dec
  implicit val schCommRev = fixers4.schema
}
object implicitFixers5 {
  implicit val encUpdate = fixers5.enc
  implicit val decUpdate = fixers5.dec
  implicit val schUpdate = fixers5.schema
}
object implicitFixers6 {
  implicit val encEvent = fixers6.enc
  implicit val decEvent = fixers6.dec
  implicit val schEvent = fixers6.schema
}

object implicitFixers7 {
  implicit val encHistResp1 = fixers7_11.enc1
  implicit val decHistResp1 = fixers7_12.dec1
  implicit val schHistResp1 = fixers7_13.schema1

  implicit val encHistResp2 = fixers7_21.enc2
  implicit val decHistResp2 = fixers7_22.dec2
  implicit val schHistResp2 = fixers7_23.schema2
}

object implicitFixers8 {
  implicit val encHist1 = fixers8_11.enc1
  implicit val decHist1 = fixers8_12.dec1
  implicit val schHist1 = fixers8_13.schema1

  implicit val encHist2 = fixers8_21.enc2
  implicit val decHist2 = fixers8_22.dec2
  implicit val schHist2 = fixers8_23.schema2
}

object Serializers {
  import derivations._
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import implicitFixers8._

  import Serializers3._

  class URainbowCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowCommand]
  class URainbowResponsePersist extends AvroSerializer[RainbowActorProtocol.URainbowResponse]

  class URainbowErrorPersist extends AvroSerializer[RainbowActorProtocol.URainbowError]
  class URainbowGeneralErrorPersist extends AvroSerializer[RainbowActorProtocol.URainbowGeneralError]

}

object Serializers2 {
  import derivations._
  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import implicitFixers8._

  import Serializers3._

  class URainbowGetCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetCommand]
  class URainbowGetIDCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetIDCommand]

  class URainbowOffsetCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetOffsetCommand]
  class URainbowMessageCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetMessageCommand]
  class URainbowHistoryCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowHistoryCommand]
  class URainbowModifyCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowModifyCommand]
  class URainbowCasModifyCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowCasModifyCommand]
  class URainbowEventPersist extends AvroSerializer[URainbowEvent]
  class UpdateRainbowPersist extends AvroSerializer[UpdateRainbow]
  class URainbowModifyListPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowModifyList]

}
object Serializers3 {
  import derivations._

  import implicitFixers1._
  import implicitFixers2._
  import implicitFixers2_0._
  import implicitFixers2_1._
  import implicitFixers2_3._
  import implicitFixers3._
  import implicitFixers4._
  import implicitFixers5._
  import implicitFixers6._
  import implicitFixers7._
  import implicitFixers8._

  class RainbowResponsePersist extends AvroSerializer[RainbowActorProtocol.RainbowResponse]
  class RainbowResponseHistoryPersist extends AvroSerializer[RainbowActorProtocol.RainbowHistoryResponse]
  class RainbowResponseHistoryParentsPersist
      extends AvroSerializer[RainbowActorProtocol.RainbowHistoryWithParentResponse]

}

object EncodersDecoders
