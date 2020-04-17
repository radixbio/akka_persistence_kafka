package com.radix.shared.persistence.serializations.util.prism

import java.util.UUID

import com.radix.shared.defs.pipettingrobot.OpentronsPlateModel.{OriginOffsetUnits, PlatePropertiesUnits, WellID, WellPropertiesUnits}
import com.radix.shared.util.prism._
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import io.circe.parser.parse
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import scalaz.Functor
import ujson.{Js, read}

import scala.collection.JavaConverters._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.util.prism._
import com.radix.shared.util.prism.implicits._
import squants.space.Volume

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
  NOTE: this code doesn't do any reordering besides the fix rollup. Move your recursive type to as far to the end of your
  parameter list if this is giving you issues at runtime.
  it is possible to generalize this, but it was easier to reorder the AST itself in the case that wrote this.
  TODO add support for field reordering to respect rolled-up field reference ordering
   */
  implicit def SchemaForFix[F[_]: Functor](implicit ev: SchemaFor[F[REPLACE.type]]): SchemaFor[Fix[F]] = {
    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName = REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")
    val in = SchemaFor[F[REPLACE.type]].schema(fieldMapper).toString

    val json = parse(in).toOption.get.asArray.get
    val names = json.map(_.hcursor.downField("name").as[String].toOption.get)
    val namespaces = json.map(_.hcursor.downField("namespace").as[String].toOption.get)

    //One of the forms REPLACE can take, since it can get serialized as a case class by avro4s for some reason when it's in a list-like type
    val replaceStr =
      s"""{"type":"record","name":"$replaceName","namespace":"$replaceNamespace","fields":[]}""".stripMargin
    //The straightforward type replace
    val replaceStr2 =
      s""""$replaceNamespace.$replaceName"""".stripMargin
    val replacement = namespaces.zip(names).map(x => s"${x._1}.${x._2}").mkString("[\"", "\",\"", "\"]")



    val lastFullReplaced =
      json.last.noSpaces.replaceAllLiterally(replaceStr, replacement).replaceAllLiterally(replaceStr2, replacement)
    val lastName = "\"" + namespaces.last + "." + names.last + "\""
    val result = json.init.foldRight((lastFullReplaced, lastName))({
      case (json, (accumSchema, lastname)) =>
        (json.noSpaces
          .replaceAllLiterally(replaceStr, replacement)
          .replaceAllLiterally(replaceStr2, replacement)
          .replaceFirst(lastname, accumSchema),
          "\"" + json.hcursor.downField("namespace").as[String].toOption.get + "." + json.hcursor
            .downField("name")
            .as[String]
            .toOption
            .get + "\"")
    })._1
    val toplevelunion = "[" + result + ",\"" + namespaces.tail.zip(names.tail).map(x => s"${x._1}.${x._2}").mkString("", "\",\"", "\"]")
    new SchemaFor[Fix[F]] {
      override def schema(fieldMapper: FieldMapper): Schema =
        new Schema.Parser().setValidate(true).parse(toplevelunion.replaceAllLiterally(s"__$replaceName", ""))
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
          case Well(_,  id, props, _, _) => {
            record.put("id", Encoder[WellID].encode(id, SchemaFor[WellID].schema(fieldMapper),fieldMapper))
            record.put("props", Encoder[WellPropertiesUnits].encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
          }
          case HeatableWell(_, id, props, tempProps, _, _) => {
            record.put("id", Encoder[WellID].encode(id, SchemaFor[WellID].schema(fieldMapper), fieldMapper))
            record.put("props", Encoder[WellPropertiesUnits].encode(props, AvroSchema[WellPropertiesUnits],fieldMapper))
            record.put("tempProps", Encoder[WellTempProperties].encode(tempProps, AvroSchema[WellTempProperties], fieldMapper))
          }
          case Fluid(_, volume, _, _) => {
            record.put("volume", Encoder[Volume].encode(volume, AvroSchema[Volume], fieldMapper))

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
          case "Fluid" => {
            val vol = Decoder[Volume].decode(record.get("volume"), AvroSchema[Volume], fieldMapper)
            new Fluid[GenericRecord](offset, volume = vol, contains, uuid)
          }
          case "Well" => {
            val wid   = Decoder[WellID].decode(record.get("id"), AvroSchema[WellID], fieldMapper)
            val props = Decoder[WellPropertiesUnits].decode(record.get("props"), AvroSchema[WellPropertiesUnits], fieldMapper)
            new Well[GenericRecord](offset, wid, props, contains, uuid)
          }
          case "HeatableWell" => {
            val wid   = Decoder[WellID].decode(record.get("id"), AvroSchema[WellID], fieldMapper)
            val props = Decoder[WellPropertiesUnits].decode(record.get("props"), AvroSchema[WellPropertiesUnits], fieldMapper)
            val tempProps =
              Decoder[WellTempProperties].decode(record.get("tempProps"), AvroSchema[WellTempProperties], fieldMapper)
            new HeatableWell[GenericRecord](offset, wid, props, tempProps, contains, uuid)
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

    implicit def AvroPath(implicit ev: SchemaFor[Fix[Path]]): Encoder[Fix[Path]] with Decoder[Fix[Path]] = {
    new Encoder[Fix[Path]] with Decoder[Fix[Path]] {

      private[this] val arraySchema = Schema.createArray(ev.schema(fieldMapper))
      private[this] val encoderAlg: Algebra[Path, AnyRef] = path => {
        val recordSchema = ev.schema(fieldMapper).getTypes.get(ev.schema(fieldMapper).getIndexNamed(path.getClass.getName))
        val record       = new GenericData.Record(recordSchema)
        path match {
          case Take(which, next) =>
            record.put("which", Encoder[Fix[Container]].encode(which, SchemaFor[Fix[Container]].schema(fieldMapper), fieldMapper))
            record.put("next", next)

          case Terminal(next) =>
            record.put("next", Encoder[Fix[Container]].encode(next, SchemaFor[Fix[Container]].schema(fieldMapper), fieldMapper))
          case _ => Unit
        }
        record
      }

      private[this] val decoderCoalgebra: Coalgebra[Path, GenericRecord] = record => {

        record.getSchema.getName match {
          case "Take" => {
            val next = record.get("next").asInstanceOf[GenericRecord]
            val which = Decoder[Fix[Container]].decode(record.get("which"), AvroSchema[Fix[Container]], fieldMapper)
            new Take[GenericRecord](which, next)
          }
          case "Terminal" => {
            val next = Decoder[Fix[Container]].decode(record.get("next"), AvroSchema[Fix[Container]], fieldMapper)
            new Terminal[GenericRecord](next)
          }
        }
      }


      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): Fix[Path] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Path]](decoderCoalgebra)
      }
      override def encode(t: Fix[Path], schema: Schema, fieldMapper: FieldMapper): AnyRef = t.cata(encoderAlg)
    }
    }


}