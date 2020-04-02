package com.radix.shared.persistence.serializations.util.prism

import java.util.UUID

import com.radix.shared.defs.pipettingrobot.OpentronsPlateModel.{OriginOffsetUnits, PlatePropertiesUnits, WellID, WellPropertiesUnits}
import com.radix.shared.util.prism._
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import scalaz.Functor
import ujson.{Js, read}
import scala.collection.JavaConverters._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.util.prism._
import com.radix.shared.util.prism.implicits._

object derivations {
  implicit val fieldMapper: FieldMapper = DefaultFieldMapper


  implicit object SchemaForPlatePropertiesWithUnits
    extends SchemaFor[PlatePropertiesUnits]
      with Encoder[PlatePropertiesUnits]
      with Decoder[PlatePropertiesUnits] {
    override def schema(fieldMapper: FieldMapper): Schema = SchemaBuilder
      .builder()
      .record("plateproperties")
      .namespace("com.radix.shared.defs.pipettingrobot.OpentronsPlateModel")
      .fields()
      .name("locations")
      .`type`()
      .map()
      .values(AvroSchema[WellPropertiesUnits])
      .mapDefault(Map.empty[String, WellPropertiesUnits].asJava)
      .name("origin_offset")
      .`type`(AvroSchema[Option[OriginOffsetUnits]])
      .withDefault(Encoder[Option[OriginOffsetUnits]].encode(None, AvroSchema[Option[OriginOffsetUnits]], fieldMapper))
      .endRecord()

    override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): PlatePropertiesUnits = {
      val record = value.asInstanceOf[GenericRecord]
      val locs = record
        .get("locations")
        .asInstanceOf[java.util.HashMap[Utf8, WellPropertiesUnits]]
        .asScala
        .toSeq
        .map(tup => (WellID(tup._1.toString), tup._2))
        .toMap
      val origin_offset =
        Decoder[Option[OriginOffsetUnits]].decode(record.get("origin_offset"), AvroSchema[Option[OriginOffsetUnits]], fieldMapper)
      PlatePropertiesUnits(origin_offset, locs)
    }

    override def encode(t: PlatePropertiesUnits, schema: Schema, fieldMapper: FieldMapper): AnyRef = {
      val record = new GenericData.Record(schema)
      record.put(
        "locations",
        implicitly[Encoder[Map[String, WellPropertiesUnits]]]
          .encode(t.locations.toSeq.map(tup => (tup._1.id, tup._2)).toMap, AvroSchema[Map[String, WellPropertiesUnits]], fieldMapper)
      )
      record.put("origin_offset",
        Encoder[Option[OriginOffsetUnits]].encode(t.origin_offset, AvroSchema[Option[OriginOffsetUnits]], fieldMapper))
      record
    }
  }

  private[this] final case object REPLACE

  /*
  This implicit conversion enables Fixpoint type serialzation.
  Avro does not allow mutually recursive type definitions (https://issues.apache.org/jira/browse/AVRO-530)
  Avro does not allow named unions (https://issues.apache.org/jira/browse/AVRO-248).
  Using the approach in AVRO-530 as a temporary workaround, we reorder the schema to have no forward dependencies
  TODO this does not support multiple recursive calls, only one
   */
  implicit def SchemaForFix[F[_]: Functor](implicit ev: SchemaFor[F[REPLACE.type]]): SchemaFor[Fix[F]] = {
    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName      = REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")
    val recordsInF       = read(SchemaFor[F[REPLACE.type]].schema(fieldMapper).toString).arr
    val names            = recordsInF.toList.map(_("name").str).map(_.toString)
    val namespaces       = recordsInF.toList.map(_("namespace").str).map(_.toString)
    //One of the forms REPLACE can take, since it can get serialized as a case class by avro4s for some reason
    val replaceStr =
      s"""{"type":"record","name":"$replaceName","namespace":"$replaceNamespace","fields":[]}""".stripMargin
    //The straightforward type replace
    val replaceStr2 =
      s""""$replaceNamespace.$replaceName"""".stripMargin
    def replacePlaceholderTermsInFieldsWithUnion(inputObject: Js, accum: Js): Js = {
      // To figure out where to splice into the list
      val lastName = names.indexOf(accum("name").str)
      // package up the union type, with things to be declared, the struct defn, and then defns from inside the struct defn
      val endDefns = accum.toString + ",\"" + names
        .drop(lastName + 1)
        .zip(namespaces)
        .map(tup => tup._2 + "." + tup._1)
        .mkString("\",\"")
      val definedSoFar = endDefns match {
        case x if x.endsWith(",\"") =>
          "[\"" + names
            .take(lastName)
            .zip(namespaces)
            .map(tup => tup._2 + "." + tup._1)
            .mkString("\",\"") + "\"," + accum.toString + "]"
        case els =>
          "[\"" + names
            .take(lastName)
            .zip(namespaces)
            .map(tup => tup._2 + "." + tup._1)
            .mkString("\",\"") + "\"," + els + "\"]"
      }

      val replacementArr = inputObject("fields").arr.toList.foldLeft(List.empty[Js.Value])({
        case (orderedFieldTypes, field) => {
          val replacement =
            field.toString.replaceAllLiterally(replaceStr, definedSoFar).replaceAllLiterally(replaceStr2, definedSoFar)
          if (replacement.contains(definedSoFar)) {
            // Add it to the end. it may have data dependencies in other fields
            orderedFieldTypes ::: List(read(replacement))
          } else {
            //Add it to the beginning, there shouldn't be any data dependencies on non-initialized types
            List(read(replacement)) ::: orderedFieldTypes
          }
        }
      })
      replacementArr.filter(_.toString.startsWith("\"")).toSet
      //upickle/ujson is mutable
      inputObject("fields") = replacementArr
      inputObject
    }
    val reorganizedSchema = recordsInF.toList.init.foldRight(
      read(
        recordsInF.last.toString
          .replaceAllLiterally(replaceStr, Js.JsonableSeq(names).toString)
          .replaceAllLiterally(replaceStr2, Js.JsonableSeq(names).toString)))({
      case (objectToUnion, accumulatedSchema) =>
        replacePlaceholderTermsInFieldsWithUnion(objectToUnion, accumulatedSchema)

    })
    val topNodeName = reorganizedSchema("name").str
    val result =
      Js.JsonableSeq(
        List(reorganizedSchema) ::: names
          .zip(namespaces)
          .filterNot(_._1 == topNodeName)
          // fully qualify all names
          .map(tup => Js.Str(tup._2 + "." + tup._1)))
        .toString
        .replaceAllLiterally(s"__$replaceName", "")

    new SchemaFor[Fix[F]] {
      override def schema(fieldMapper: FieldMapper): Schema =
        new Schema.Parser().setValidate(true).parse(result)
    }

  }

  /**
   * There's probably a much nicer way to do this given labelledGeneric. TODO explore this to generalize
   * @param ev
   * @return
   */
  implicit def EncoderForContainer(
                                    implicit ev: SchemaFor[Fix[Container]]): Encoder[Fix[Container]] with Decoder[Fix[Container]] =
    new Encoder[Fix[Container]] with Decoder[Fix[Container]] {
      private[this] val arraySchema = Schema.createArray(ev.schema(fieldMapper))
      private[this] val encoderAlg: Algebra[Container, AnyRef] = cont => {
        val recordSchema = ev.schema(fieldMapper).getTypes.get(ev.schema(fieldMapper).getIndexNamed(cont.getClass.getName))
        val record       = new GenericData.Record(recordSchema)
        val containsr    = new GenericData.Array[AnyRef](arraySchema, cont.contains.asJava)
        record.put("offset", Encoder[Offset].encode(cont.offset, SchemaFor[Offset].schema(fieldMapper), fieldMapper))
        record.put("uuid", Encoder[UUID].encode(cont.uuid, SchemaFor[UUID].schema(fieldMapper), fieldMapper))
        record.put("contains", containsr)
        cont match {
          case Plate(_, props, _, _) => {
            record.put("props", Encoder[PlatePropertiesUnits].encode(props, AvroSchema[PlatePropertiesUnits], fieldMapper))
          }
          case Well(_, _, id, props, _) => {
            record.put("id", Encoder[WellID].encode(id, SchemaFor[WellID].schema(fieldMapper),fieldMapper))
            record.put("props", Encoder[WellPropertiesUnits].encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
          }
          case HeatableWell(_, _, id, props, tempProps, _) => {
            record.put("id", Encoder[WellID].encode(id, SchemaFor[WellID].schema(fieldMapper), fieldMapper))
            record.put("props", Encoder[WellPropertiesUnits].encode(props, AvroSchema[WellPropertiesUnits],fieldMapper))
            record.put("tempProps", Encoder[WellTempProperties].encode(tempProps, AvroSchema[WellTempProperties], fieldMapper))
          }

          case _ => Unit
        }
        record
      }
      override def encode(t: Fix[Container], schema: Schema, fieldMapper: FieldMapper): AnyRef = t.cata(encoderAlg)

      private[this] val decoderCoalgebra: Coalgebra[Container, GenericRecord] = record => {
        val uuid     = Decoder[UUID].decode(record.get("uuid"), AvroSchema[UUID],fieldMapper)
        val offset   = Decoder[Offset].decode(record.get("offset"), AvroSchema[Offset], fieldMapper)
        val contains = record.get("contains").asInstanceOf[java.util.Collection[GenericRecord]].asScala.toList
        record.getSchema.getName match {
          case "Fluid" => new Fluid[GenericRecord](offset, contains, uuid)
          case "Well" => {
            val wid   = Decoder[WellID].decode(record.get("id"), AvroSchema[WellID], fieldMapper)
            val props = Decoder[WellPropertiesUnits].decode(record.get("props"), AvroSchema[WellPropertiesUnits], fieldMapper)
            new Well[GenericRecord](offset, contains, wid, props, uuid)
          }
          case "HeatableWell" => {
            val wid   = Decoder[WellID].decode(record.get("id"), AvroSchema[WellID], fieldMapper)
            val props = Decoder[WellPropertiesUnits].decode(record.get("props"), AvroSchema[WellPropertiesUnits], fieldMapper)
            val tempProps =
              Decoder[WellTempProperties].decode(record.get("tempProps"), AvroSchema[WellTempProperties], fieldMapper)
            new HeatableWell[GenericRecord](offset, contains, wid, props, tempProps, uuid)
          }
          case "Plate" => {
            val props =
              Decoder[PlatePropertiesUnits].decode(record.get("props"), AvroSchema[PlatePropertiesUnits], fieldMapper)
            new Plate[GenericRecord](offset, props, contains, uuid)
          }
          case "Robot" => new Robot[GenericRecord](offset, contains, uuid)
          case "World" => new World[GenericRecord](offset, contains, uuid)
          case "Shim"  => new Shim[GenericRecord](offset, contains, uuid)
        }
      }

      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Fix[Container] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Container]](decoderCoalgebra)
      }
    }


}