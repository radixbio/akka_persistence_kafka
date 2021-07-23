package com.radix.shared.persistence.serializations.utils.rainbow

import java.util.UUID

import akka.actor.ExtendedActorSystem
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, ImmutableRecord, SchemaFor}
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import com.radix.shared.persistence.serializations.squants.schemas._

import scala.collection.JavaConverters._
import java.util

import akka.persistence.SnapshotMetadata
import com.radix.rainbow._
import com.radix.rainbow.URainbow.{CommandMetadata, RainbowCommandAndReverse, RainbowHistoricalResponse, RainbowHistoricalResponseWithParents, RainbowModifyCommand, URainbowEvent, UpdateRainbow, ValidReverse}
import com.radix.rainbow.URainbow.UTypes.{ID, Metadata, RainbowMetaSerialized}
import com.radix.shared.persistence.AvroSerializer
import com.radix.utils.rainbowuservice.RainbowActorProtocol
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.utils.rainbowuservice.RainbowActorProtocol.{RainbowHistoryResponse, RainbowHistoryWithParentResponse, URainbowCommand, URainbowResponse} //this is necessary

object derivations {
//  implicit val fieldMapper: FieldMapper = DefaultFieldMapper
//
//  implicit def mapSchemaForUUID[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[UUID, V]] = {
//    new SchemaFor[Map[UUID, V]] {
//      override def schema: Schema =
//        SchemaBuilder.map().values(schemaFor.schema)
//      override def fieldMapper: FieldMapper = DefaultFieldMapper
//    }
//  }
//
//  implicit def mapDecoderUUID[T](implicit valueDecoder: Decoder[T], schemaForImp: SchemaFor[T]): Decoder[Map[UUID, T]] =
//    new Decoder[Map[UUID, T]] {
//
//      override def decode(value: Any): Map[UUID, T] =
//        value match {
//          case map: java.util.Map[_, _] =>
//            map.asScala.toMap.map {
//              case (k, v) =>
//                UUID.fromString(
//                  implicitly[Decoder[String]]
//                    .decode(k)
//                ) -> valueDecoder.decode(v)
//            }
//          case other => sys.error("Unsupported map " + other)
//        }
//      override def schemaFor: SchemaFor[Map[UUID, T]] = mapSchemaForUUID[T]
//    }
//
//  implicit def mapEncoderUUID[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[UUID, V]] =
//    new Encoder[Map[UUID, V]] {
//
//      override def encode(map: Map[UUID, V]): java.util.Map[String, AnyRef] = {
//        require(schema != null)
//        val java = new util.HashMap[String, AnyRef]
//        map.foreach {
//          case (k, v) =>
//            java.put(k.toString, encoder.encode(v))
//        }
//        java
//      }
//
//      override def schemaFor: SchemaFor[Map[UUID, V]] = mapSchemaForUUID[V]
//    }
//
  implicit def mapSchemaForInt[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[Int, V]] = {
    new SchemaFor[Map[Int, V]] {
      override def schema: Schema = AvroSchema[Map[String, V]]
      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }
  }
  implicit def mapDecoderInt[T](implicit valueDecoder: Decoder[T], schemaForImp: SchemaFor[T]): Decoder[Map[Int, T]] =
    new Decoder[Map[Int, T]] {
      override def decode(value: Any): Map[Int, T] =
        Decoder[Map[String, T]].decode(value).map({ case (k, v) => k.toInt -> v })
      override def schemaFor: SchemaFor[Map[Int, T]] = implicitly[SchemaFor[Map[Int, T]]]
    }

  implicit def mapEncoderInt[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[Int, V]] =
    new Encoder[Map[Int, V]] {
      override def encode(map: Map[Int, V]): AnyRef =
        Encoder[Map[String, V]].encode(map.map({ case (k, v) => k.toString -> v }))
      override def schemaFor: SchemaFor[Map[Int, V]] = implicitly[SchemaFor[Map[Int, V]]]
    }
}

////From here down, all is horrible. Each of this schemas and encoders are so large
////that if they are in the same object, they go over the jvm limit of 64kb per file
////they are separated out of necessity

object PrimitiveSerializers {}

object ManualSerializers {
  import derivations._
  implicit val URainbowGeneralErrorS: SchemaFor[RainbowActorProtocol.URainbowGeneralError] =
    new SchemaFor[RainbowActorProtocol.URainbowGeneralError] {
      override def schema: Schema =
        SchemaBuilder
          .record("URainbowGeneralError")
          .namespace("com.radix.utils.rainbowuservice.RainbowActorProtocol")
          .fields()
          .name("message")
          .`type`(AvroSchema[String])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }
  implicit val URainbowGeneralErrorE: Encoder[RainbowActorProtocol.URainbowGeneralError] =
    new Encoder[RainbowActorProtocol.URainbowGeneralError] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.URainbowGeneralError]]
      override def encode(value: RainbowActorProtocol.URainbowGeneralError): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("message", Encoder[String].encode(value.message))
        R
      }

      override def schemaFor: SchemaFor[RainbowActorProtocol.URainbowGeneralError] = S

    }
  implicit val URainbowGeneralErrorD: Decoder[RainbowActorProtocol.URainbowGeneralError] =
    new Decoder[RainbowActorProtocol.URainbowGeneralError] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.URainbowGeneralError]]
      override def decode(value: Any): RainbowActorProtocol.URainbowGeneralError =
        RainbowActorProtocol.URainbowGeneralError(
          Decoder[String].decode(value.asInstanceOf[GenericRecord].get("message"))
        )

      override def schemaFor: SchemaFor[RainbowActorProtocol.URainbowGeneralError] = S
    }

  implicit val SerialRainbowS: SchemaFor[RainbowMetaSerialized] =
    new SchemaFor[RainbowMetaSerialized] {
      override def schema: Schema =
        SchemaBuilder
          .record("SerialRainbow")
          .namespace("com.radix.rainbow")
          .fields()
          .name("mapIdToParent")
          .`type`(AvroSchema[Map[String, Option[String]]])
          .noDefault()
          .name("mapIdToElement")
          .`type`(AvroSchema[Map[String, LabMeta[Metadata]]])
          //          .map()
          //          .values(AvroSchema[LabMeta[Metadata]])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val SerialRainbowE: Encoder[RainbowMetaSerialized] = new Encoder[RainbowMetaSerialized] {
    val S = implicitly[SchemaFor[RainbowMetaSerialized]]
    override def encode(value: RainbowMetaSerialized): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("mapIdToParent", Encoder[Map[String, Option[String]]].encode(value.mapIdToParent))
      R.put("mapIdToElement", Encoder[Map[String, LabMeta[Metadata]]].encode(value.mapIdToElement))
      R
    }

    override def schemaFor: SchemaFor[RainbowMetaSerialized] = S
  }

  implicit val SerialRainbowD: Decoder[RainbowMetaSerialized] = new Decoder[RainbowMetaSerialized] {
    val S = implicitly[SchemaFor[RainbowMetaSerialized]]
    override def decode(value: Any): RainbowMetaSerialized = {
      val R = value.asInstanceOf[GenericRecord]
      val mapIdToParent = Decoder[Map[String, Option[String]]].decode(R.get("mapIdToParent"))
      val mapIdToElement = Decoder[Map[String, LabMeta[Metadata]]].decode(R.get("mapIdToElement"))
      SerialRainbow(mapIdToParent, mapIdToElement)
    }

    override def schemaFor: SchemaFor[RainbowMetaSerialized] = S
  }

  implicit val CasS: SchemaFor[URainbow.Cas] = new SchemaFor[URainbow.Cas] {
    override def schema: Schema =
      SchemaBuilder
        .record("Cas")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("prevRainbow")
        .`type`(AvroSchema[RainbowMetaSerialized])
        .noDefault()
        .name("newRainbow")
        .`type`(AvroSchema[RainbowMetaSerialized])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val CasE: Encoder[URainbow.Cas] = new Encoder[URainbow.Cas] {
    val S = implicitly[SchemaFor[URainbow.Cas]]
    override def encode(value: URainbow.Cas): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("prevRainbow", Encoder[RainbowMetaSerialized].encode(value.prevRainbow))
      R.put("newRainbow", Encoder[RainbowMetaSerialized].encode(value.newRainbow))
      R
    }

    override def schemaFor: SchemaFor[URainbow.Cas] = S
  }

  implicit val CasD: Decoder[URainbow.Cas] = new Decoder[URainbow.Cas] {
    val S = implicitly[SchemaFor[URainbow.Cas]]
    override def decode(value: Any): URainbow.Cas = {
      val R = value.asInstanceOf[GenericRecord]
      val prevRainbow = Decoder[RainbowMetaSerialized].decode(R.get("prevRainbow"))
      val newRainbow = Decoder[RainbowMetaSerialized].decode(R.get("newRainbow"))
      URainbow.Cas(prevRainbow, newRainbow)
    }

    override def schemaFor: SchemaFor[URainbow.Cas] = S
  }

  implicit val InsertS: SchemaFor[URainbow.Insert] = new SchemaFor[URainbow.Insert] {
    override def schema: Schema =
      SchemaBuilder
        .record("Insert")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("component")
        .`type`(AvroSchema[LabMeta[Metadata]])
        .noDefault()
        .name("id")
        .`type`(AvroSchema[String])
        .noDefault()
        .name("location")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val InsertE: Encoder[URainbow.Insert] = new Encoder[URainbow.Insert] {
    val S = implicitly[SchemaFor[URainbow.Insert]]
    override def encode(value: URainbow.Insert): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("component", Encoder[LabMeta[Metadata]].encode(value.component))
      R.put("id", Encoder[String].encode(value.id))
      R.put("location", Encoder[Option[String]].encode(value.location))
      R
    }

    override def schemaFor: SchemaFor[URainbow.Insert] = S
  }

  implicit val InsertD: Decoder[URainbow.Insert] = new Decoder[URainbow.Insert] {
    val S = implicitly[SchemaFor[URainbow.Insert]]
    override def decode(value: Any): URainbow.Insert = {
      val R = value.asInstanceOf[GenericRecord]
      val component = Decoder[LabMeta[Metadata]].decode(R.get("component"))
      val id = Decoder[ID].decode(R.get("id"))
      val location = Decoder[Option[String]].decode(R.get("location"))
      URainbow.Insert(component, id, location)
    }

    override def schemaFor: SchemaFor[URainbow.Insert] = S
  }

  implicit val RemoveS: SchemaFor[URainbow.Remove] = new SchemaFor[URainbow.Remove] {
    override def schema: Schema =
      SchemaBuilder
        .record("Remove")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("id")
        .`type`(AvroSchema[String])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val RemoveE: Encoder[URainbow.Remove] = new Encoder[URainbow.Remove] {

    val S = implicitly[SchemaFor[URainbow.Remove]]
    override def encode(value: URainbow.Remove): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("id", Encoder[String].encode(value.id))
      R
    }

    override def schemaFor: SchemaFor[URainbow.Remove] = S
  }

  implicit val RemoveD: Decoder[URainbow.Remove] = new Decoder[URainbow.Remove] {
    val S = implicitly[SchemaFor[URainbow.Remove]]
    override def decode(value: Any): URainbow.Remove = {
      val R = value.asInstanceOf[GenericRecord]
      URainbow.Remove(Decoder[String].decode(R.get("id")))
    }

    override def schemaFor: SchemaFor[URainbow.Remove] = S
  }

  implicit val RelocateS: SchemaFor[URainbow.Relocate] = new SchemaFor[URainbow.Relocate] {
    override def schema: Schema =
      SchemaBuilder
        .record("Relocate")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("id")
        .`type`(AvroSchema[String])
        .noDefault()
        .name("location")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val RelocateE: Encoder[URainbow.Relocate] = new Encoder[URainbow.Relocate] {
    val S = implicitly[SchemaFor[URainbow.Relocate]]
    override def encode(value: URainbow.Relocate): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("id", Encoder[String].encode(value.id))
      R.put("location", Encoder[Option[String]].encode(value.location))
      R
    }

    override def schemaFor: SchemaFor[URainbow.Relocate] = S
  }

  implicit val RelocateD: Decoder[URainbow.Relocate] = new Decoder[URainbow.Relocate] {
    val S = implicitly[SchemaFor[URainbow.Relocate]]
    override def decode(value: Any): URainbow.Relocate = {
      val R = value.asInstanceOf[GenericRecord]
      val id = Decoder[String].decode(R.get("id"))
      val location = Decoder[Option[String]].decode(R.get("location"))
      URainbow.Relocate(id, location)
    }

    override def schemaFor: SchemaFor[URainbow.Relocate] = S
  }

  implicit val ReplaceS: SchemaFor[URainbow.Replace] = new SchemaFor[URainbow.Replace] {
    override def schema: Schema =
      SchemaBuilder
        .record("Replace")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("oldId")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .name("newId")
        .`type`(AvroSchema[String])
        .noDefault()
        .name("component")
        .`type`(AvroSchema[LabMeta[Metadata]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val ReplaceE: Encoder[URainbow.Replace] = new Encoder[URainbow.Replace] {
    val S = implicitly[SchemaFor[URainbow.Replace]]
    override def encode(value: URainbow.Replace): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("oldId", Encoder[Option[String]].encode(value.oldId))
      R.put("newId", Encoder[String].encode(value.newId))
      R.put("component", Encoder[LabMeta[Metadata]].encode(value.component))
      R
    }

    override def schemaFor: SchemaFor[URainbow.Replace] = S
  }

  implicit val ReplaceD: Decoder[URainbow.Replace] = new Decoder[URainbow.Replace] {
    val S = implicitly[SchemaFor[URainbow.Replace]]
    override def decode(value: Any): URainbow.Replace = {
      val R = value.asInstanceOf[GenericRecord]
      val oldId = Decoder[Option[String]].decode(R.get("oldId"))
      val newId = Decoder[String].decode(R.get("newId"))
      val component = Decoder[LabMeta[Metadata]].decode(R.get("component"))
      URainbow.Replace(oldId, newId, component)
    }

    override def schemaFor: SchemaFor[URainbow.Replace] = S
  }

  implicit val MultiOpS: SchemaFor[URainbow.MultiOp] = new SchemaFor[URainbow.MultiOp] {
    override def schema: Schema =
      SchemaBuilder
        .record("MultiOp")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("removed")
        .`type`(AvroSchema[Seq[URainbow.Remove]])
        .withDefault(Encoder[Seq[URainbow.Remove]].encode(Seq.empty))
        .name("relocate")
        .`type`(AvroSchema[Seq[URainbow.Relocate]])
        .withDefault(Encoder[Seq[URainbow.Relocate]].encode(Seq.empty))
        .name("replace")
        .`type`(AvroSchema[Seq[URainbow.Replace]])
        .withDefault(Encoder[Seq[URainbow.Relocate]].encode(Seq.empty))
        .name("insert")
        .`type`(AvroSchema[Seq[URainbow.Insert]])
        .withDefault(Encoder[Seq[URainbow.Insert]].encode(Seq.empty))
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val MultiOpE: Encoder[URainbow.MultiOp] = new Encoder[URainbow.MultiOp] {
    val S = implicitly[SchemaFor[URainbow.MultiOp]]
    override def encode(value: URainbow.MultiOp): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("removed", Encoder[Seq[URainbow.Remove]].encode(value.removed))
      R.put("relocate", Encoder[Seq[URainbow.Relocate]].encode(value.relocate))
      R.put("replace", Encoder[Seq[URainbow.Replace]].encode(value.replace))
      R.put("insert", Encoder[Seq[URainbow.Insert]].encode(value.insert))
      R
    }

    override def schemaFor: SchemaFor[URainbow.MultiOp] = S
  }

  implicit val MultiOpD: Decoder[URainbow.MultiOp] = new Decoder[URainbow.MultiOp] {
    val S = implicitly[SchemaFor[URainbow.MultiOp]]
    override def decode(value: Any): URainbow.MultiOp = {
      val R = value.asInstanceOf[GenericRecord]
      val removed = Decoder[Seq[URainbow.Remove]].decode(R.get("removed"))
      val relocate = Decoder[Seq[URainbow.Relocate]].decode(R.get("relocate"))
      val replace = Decoder[Seq[URainbow.Replace]].decode(R.get("replace"))
      val insert = Decoder[Seq[URainbow.Insert]].decode(R.get("insert"))
      URainbow.MultiOp(removed, relocate, replace, insert)
    }

    override def schemaFor: SchemaFor[URainbow.MultiOp] = S
  }

  implicit val StringMetaS: SchemaFor[URainbow.StringMeta] = new SchemaFor[URainbow.StringMeta] {
    override def schema: Schema =
      SchemaBuilder
        .record("StringMeta")
        .namespace("com.radix.rainbow.URainbow")
        .fields
        .name("message")
        .`type`(AvroSchema[String])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val StringMetaE: Encoder[URainbow.StringMeta] = new Encoder[URainbow.StringMeta] {
    val S = implicitly[SchemaFor[URainbow.StringMeta]]
    override def encode(value: URainbow.StringMeta): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("message", Encoder[String].encode(value.message))
      R
    }

    override def schemaFor: SchemaFor[URainbow.StringMeta] = S
  }

  implicit val StringMetaD: Decoder[URainbow.StringMeta] = new Decoder[URainbow.StringMeta] {
    val S = implicitly[SchemaFor[URainbow.StringMeta]]
    override def decode(value: Any): URainbow.StringMeta = {
      val R = value.asInstanceOf[GenericRecord]
      val message = Decoder[String].decode(R.get("message"))
      URainbow.StringMeta(message)
    }

    override def schemaFor: SchemaFor[URainbow.StringMeta] = S
  }

  implicit val InsertRainbowS: SchemaFor[URainbow.InsertRainbow] = new SchemaFor[URainbow.InsertRainbow] {
    override def schema: Schema =
      SchemaBuilder
        .record("InsertRainbow")
        .namespace("com.radix.rainbow.URainbow")
        .fields
        .name("component")
        .`type`(AvroSchema[RainbowMetaSerialized])
        .noDefault
        .name("location")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val InsertRainbowE: Encoder[URainbow.InsertRainbow] = new Encoder[URainbow.InsertRainbow] {
    val S = implicitly[SchemaFor[URainbow.InsertRainbow]]
    override def encode(value: URainbow.InsertRainbow): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("component", Encoder[RainbowMetaSerialized].encode(value.component))
      R.put("location", Encoder[Option[String]].encode(value.location))
      R
    }

    override def schemaFor: SchemaFor[URainbow.InsertRainbow] = S
  }

  implicit val InsertRainbowD: Decoder[URainbow.InsertRainbow] = new Decoder[URainbow.InsertRainbow] {
    val S = implicitly[SchemaFor[URainbow.InsertRainbow]]
    override def decode(value: Any): URainbow.InsertRainbow = {
      val R = value.asInstanceOf[GenericRecord]
      val component = Decoder[RainbowMetaSerialized].decode(R.get("component"))
      val location = Decoder[Option[String]].decode(R.get("location"))
      URainbow.InsertRainbow(component, location)
    }

    override def schemaFor: SchemaFor[URainbow.InsertRainbow] = S
  }

  implicit val RemoveAndMakeChildrenRootS: SchemaFor[URainbow.RemoveAndMakeChildrenRoot] =
    new SchemaFor[URainbow.RemoveAndMakeChildrenRoot] {
      override def schema: Schema =
        SchemaBuilder
          .record("RemoveAndMakeChildrenRoot")
          .namespace("com.radix.rainbow.URainbow")
          .fields()
          .name("id")
          .`type`(AvroSchema[String])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val RemoveAndMakeChildrenRootE: Encoder[URainbow.RemoveAndMakeChildrenRoot] =
    new Encoder[URainbow.RemoveAndMakeChildrenRoot] {
      val S = implicitly[SchemaFor[URainbow.RemoveAndMakeChildrenRoot]]
      override def encode(value: URainbow.RemoveAndMakeChildrenRoot): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("id", Encoder[String].encode(value.id))
        R
      }

      override def schemaFor: SchemaFor[URainbow.RemoveAndMakeChildrenRoot] = S
    }

  implicit val RemoveAndMakeChildrenRootD: Decoder[URainbow.RemoveAndMakeChildrenRoot] =
    new Decoder[URainbow.RemoveAndMakeChildrenRoot] {
      val S = implicitly[SchemaFor[URainbow.RemoveAndMakeChildrenRoot]]
      override def decode(value: Any): URainbow.RemoveAndMakeChildrenRoot = {
        val R = value.asInstanceOf[GenericRecord]
        val id = Decoder[String].decode(R.get("id"))
        URainbow.RemoveAndMakeChildrenRoot(id)
      }

      override def schemaFor: SchemaFor[URainbow.RemoveAndMakeChildrenRoot] = S
    }

  implicit val ReplaceReverseS: SchemaFor[URainbow.ReplaceReverse] = new SchemaFor[URainbow.ReplaceReverse] {
    override def schema: Schema =
      SchemaBuilder
        .record("ReplaceReverse")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("oldId")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .name("newId")
        .`type`(AvroSchema[String])
        .noDefault()
        .name("component")
        .`type`(AvroSchema[LabMeta[Metadata]])
        .noDefault()
        .name("location")
        .`type`(AvroSchema[Option[String]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val ReplaceReverseE: Encoder[URainbow.ReplaceReverse] = new Encoder[URainbow.ReplaceReverse] {
    val S = implicitly[SchemaFor[URainbow.ReplaceReverse]]
    override def encode(value: URainbow.ReplaceReverse): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("oldId", Encoder[Option[String]].encode(value.oldId))
      R.put("newId", Encoder[String].encode(value.newId))
      R.put("component", Encoder[LabMeta[Metadata]].encode(value.component))
      R.put("location", Encoder[Option[String]].encode(value.location))
      R
    }

    override def schemaFor: SchemaFor[URainbow.ReplaceReverse] = S
  }

  implicit val ReplaceReverseD: Decoder[URainbow.ReplaceReverse] = new Decoder[URainbow.ReplaceReverse] {
    val S = implicitly[SchemaFor[URainbow.ReplaceReverse]]
    override def decode(value: Any): URainbow.ReplaceReverse = {
      val R = value.asInstanceOf[GenericRecord]
      val oldId = Decoder[Option[String]].decode(R.get("oldId"))
      val newId = Decoder[String].decode(R.get("newId"))
      val component = Decoder[LabMeta[Metadata]].decode(R.get("component"))
      val location = Decoder[Option[String]].decode(R.get("location"))
      URainbow.ReplaceReverse(oldId, newId, component, location)
    }

    override def schemaFor: SchemaFor[URainbow.ReplaceReverse] = S
  }

  implicit val RemoveMultipleS: SchemaFor[URainbow.RemoveMultiple] = new SchemaFor[URainbow.RemoveMultiple] {
    override def schema: Schema =
      SchemaBuilder
        .record("RemoveMultiple")
        .namespace("com.radix.rainbow.URainbow")
        .fields()
        .name("ids")
        .`type`(AvroSchema[Set[String]])
        .noDefault()
        .endRecord()

    override def fieldMapper: FieldMapper = DefaultFieldMapper
  }

  implicit val RemoveMultipleE: Encoder[URainbow.RemoveMultiple] = new Encoder[URainbow.RemoveMultiple] {
    val S = implicitly[SchemaFor[URainbow.RemoveMultiple]]
    override def encode(value: URainbow.RemoveMultiple): AnyRef = {
      val R = new GenericData.Record(S.schema)
      R.put("ids", Encoder[Set[String]].encode(value.ids))
      R
    }

    override def schemaFor: SchemaFor[URainbow.RemoveMultiple] = S
  }

  implicit val RemoveMultipleD: Decoder[URainbow.RemoveMultiple] = new Decoder[URainbow.RemoveMultiple] {
    val S = implicitly[SchemaFor[URainbow.RemoveMultiple]]
    override def decode(value: Any): URainbow.RemoveMultiple = {
      val R = value.asInstanceOf[GenericRecord]
      val ids = Decoder[Set[String]].decode(R.get("ids"))
      URainbow.RemoveMultiple(ids)
    }

    override def schemaFor: SchemaFor[URainbow.RemoveMultiple] = S
  }

  implicit val RainbowCommandAndReverseS: SchemaFor[URainbow.RainbowCommandAndReverse] =
    new SchemaFor[URainbow.RainbowCommandAndReverse] {
      override def schema: Schema =
        SchemaBuilder
          .record("RainbowCommandAndReverse")
          .namespace("com.radix.rainbow.URainbow")
          .fields()
          .name("comm")
          .`type`(AvroSchema[RainbowModifyCommand])
          .noDefault()
          .name("reverse")
          .`type`(AvroSchema[ValidReverse])
          .noDefault()
          .name("metadata")
          .`type`(AvroSchema[CommandMetadata])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val RainbowCommandAndReverseE: Encoder[URainbow.RainbowCommandAndReverse] =
    new Encoder[URainbow.RainbowCommandAndReverse] {
      val S = implicitly[SchemaFor[URainbow.RainbowCommandAndReverse]]
      override def encode(value: URainbow.RainbowCommandAndReverse): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("comm", Encoder[RainbowModifyCommand].encode(value.comm))
        R.put("reverse", Encoder[ValidReverse].encode(value.reverse))
        R.put("metadata", Encoder[CommandMetadata].encode(value.metadata))
        R
      }

      override def schemaFor: SchemaFor[URainbow.RainbowCommandAndReverse] = S
    }

  implicit val RainbowCommandAndReverseD: Decoder[URainbow.RainbowCommandAndReverse] =
    new Decoder[URainbow.RainbowCommandAndReverse] {
      val S = implicitly[SchemaFor[URainbow.RainbowCommandAndReverse]]
      override def decode(value: Any): URainbow.RainbowCommandAndReverse = {
        val R = value.asInstanceOf[GenericRecord]
        val comm = Decoder[RainbowModifyCommand].decode(R.get("comm"))
        val reverse = Decoder[ValidReverse].decode(R.get("reverse"))
        val metadata = Decoder[CommandMetadata].decode(R.get("metadata"))
        URainbow.RainbowCommandAndReverse(comm, reverse, metadata)
      }

      override def schemaFor: SchemaFor[URainbow.RainbowCommandAndReverse] = S
    }

  implicit val RainbowHistoricalResponseS: SchemaFor[URainbow.RainbowHistoricalResponse] =
    new SchemaFor[URainbow.RainbowHistoricalResponse] {
      override def schema: Schema =
        SchemaBuilder
          .record("RainbowHistoricalResponse")
          .namespace("com.radix.rainbow.URainbow")
          .fields()
          .name("messages")
          .`type`(AvroSchema[Map[Int, RainbowCommandAndReverse]])
          .noDefault()
          .name("edges")
          .`type`(AvroSchema[Map[Int, Seq[(Int, ID)]]])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val RainbowHistoricalResponseE: Encoder[URainbow.RainbowHistoricalResponse] =
    new Encoder[URainbow.RainbowHistoricalResponse] {
      val S = implicitly[SchemaFor[URainbow.RainbowHistoricalResponse]]
      override def encode(value: URainbow.RainbowHistoricalResponse): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("messages", Encoder[Map[Int, RainbowCommandAndReverse]].encode(value.messages))
        R.put("edges", Encoder[Map[Int, Seq[(Int, ID)]]].encode(value.edges))
        R
      }

      override def schemaFor: SchemaFor[URainbow.RainbowHistoricalResponse] = S
    }

  implicit val RainbowHistoricalResponseD: Decoder[URainbow.RainbowHistoricalResponse] =
    new Decoder[URainbow.RainbowHistoricalResponse] {
      val S = implicitly[SchemaFor[URainbow.RainbowHistoricalResponse]]
      override def decode(value: Any): URainbow.RainbowHistoricalResponse = {
        val R = value.asInstanceOf[GenericData.Record]
        val messages = Decoder[Map[Int, RainbowCommandAndReverse]].decode(R.get("messages"))
        val edges = Decoder[Map[Int, Seq[(Int, ID)]]].decode(R.get("edges"))
        URainbow.RainbowHistoricalResponse(messages, edges)
      }

      override def schemaFor: SchemaFor[URainbow.RainbowHistoricalResponse] = S
    }

  implicit val RainbowHistoryResponseS: SchemaFor[RainbowActorProtocol.RainbowHistoryResponse] =
    new SchemaFor[RainbowActorProtocol.RainbowHistoryResponse] {
      override def schema: Schema =
        SchemaBuilder
          .record("RainbowHistoryResponse")
          .namespace("com.radix.utils.rainbowuservice.RainbowActorProtocol")
          .fields()
          .name("response")
          .`type`(AvroSchema[RainbowHistoricalResponse])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val RainbowHistoryResponseE: Encoder[RainbowActorProtocol.RainbowHistoryResponse] =
    new Encoder[RainbowActorProtocol.RainbowHistoryResponse] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.RainbowHistoryResponse]]
      override def encode(value: RainbowActorProtocol.RainbowHistoryResponse): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("response", Encoder[RainbowHistoricalResponse].encode(value.response))
        R
      }

      override def schemaFor: SchemaFor[RainbowActorProtocol.RainbowHistoryResponse] = S
    }

  implicit val RainbowHistoryResponseD: Decoder[RainbowActorProtocol.RainbowHistoryResponse] =
    new Decoder[RainbowActorProtocol.RainbowHistoryResponse] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.RainbowHistoryResponse]]
      override def decode(value: Any): RainbowActorProtocol.RainbowHistoryResponse = {
        val R = value.asInstanceOf[GenericRecord]
        val response = Decoder[RainbowHistoricalResponse].decode(R.get("response"))
        RainbowActorProtocol.RainbowHistoryResponse(response)
      }

      override def schemaFor: SchemaFor[RainbowActorProtocol.RainbowHistoryResponse] = S
    }

  implicit val RainbowHistoryWithParentResponseS: SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse] =
    new SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse] {
      override def schema: Schema =
        SchemaBuilder
          .record("RainbowHistoryWithParentResponse")
          .namespace("com.radix.utils.rainbowuservice.RainbowActorProtocol")
          .fields()
          .name("response")
          .`type`(AvroSchema[RainbowHistoricalResponseWithParents])
          .noDefault()
          .endRecord()

      override def fieldMapper: FieldMapper = DefaultFieldMapper
    }

  implicit val RainbowHistoryWithParentResponseE: Encoder[RainbowActorProtocol.RainbowHistoryWithParentResponse] =
    new Encoder[RainbowActorProtocol.RainbowHistoryWithParentResponse] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse]]
      override def encode(value: RainbowActorProtocol.RainbowHistoryWithParentResponse): AnyRef = {
        val R = new GenericData.Record(S.schema)
        R.put("response", Encoder[RainbowHistoricalResponseWithParents].encode(value.response))
        R
      }

      override def schemaFor: SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse] = S
    }

  implicit val RainbowHistoryWithParentResponseD: Decoder[RainbowActorProtocol.RainbowHistoryWithParentResponse] =
    new Decoder[RainbowActorProtocol.RainbowHistoryWithParentResponse] {
      val S = implicitly[SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse]]
      override def decode(value: Any): RainbowActorProtocol.RainbowHistoryWithParentResponse = {
        val R = value.asInstanceOf[GenericRecord]
        val response = Decoder[RainbowHistoricalResponseWithParents].decode(R.get("response"))
        RainbowActorProtocol.RainbowHistoryWithParentResponse(response)
      }

      override def schemaFor: SchemaFor[RainbowActorProtocol.RainbowHistoryWithParentResponse] = S
    }

}

object Serializers {

  import ManualSerializers._
  class URainbowCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowCommand] // gen
  class URainbowResponsePersist extends AvroSerializer[RainbowActorProtocol.URainbowResponse] // gen

  class URainbowErrorPersist extends AvroSerializer[RainbowActorProtocol.URainbowError] // gen
  class URainbowGeneralErrorPersist extends AvroSerializer[RainbowActorProtocol.URainbowGeneralError] // ok

  class URainbowGetCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetCommand] // gen
  class URainbowGetIDCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetIDCommand] // gen

  class URainbowOffsetCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetOffsetCommand] // gen
  class URainbowMessageCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowGetMessageCommand] // gen
  class URainbowHistoryCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowHistoryCommand] // gen
  class URainbowModifyCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowModifyCommand] // gen
  class URainbowCasModifyCommandPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowCasModifyCommand] // gen
  class URainbowEventPersist extends AvroSerializer[URainbowEvent] // gen
  class UpdateRainbowPersist extends AvroSerializer[UpdateRainbow] // gen
  class URainbowModifyListPersist(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[RainbowActorProtocol.URainbowModifyList] // gen
  class RainbowResponsePersist extends AvroSerializer[RainbowActorProtocol.RainbowResponse] // gen
  class RainbowResponseHistoryPersist extends AvroSerializer[RainbowActorProtocol.RainbowHistoryResponse] // ok
  class RainbowResponseHistoryParentsPersist
      extends AvroSerializer[RainbowActorProtocol.RainbowHistoryWithParentResponse] // ok

  class SerialRainbowSerializer extends AvroSerializer[SerialRainbow[String, LabMeta[Metadata]]] // ok

}
