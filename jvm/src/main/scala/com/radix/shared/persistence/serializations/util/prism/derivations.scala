package com.radix.shared.persistence.serializations.util.prism

import java.util.UUID

import com.radix.shared.defs.pipettingrobot.OpentronsPlateModel.{OriginOffsetUnits, PipetteTipData, PlatePropertiesUnits, WellID, WellPropertiesUnits, WellsU, OriginOffset, WellProperties}
import com.radix.shared.util.prism._
import com.sksamuel.avro4s.{AvroSchema, Decoder, DefaultFieldMapper, Encoder, FieldMapper, SchemaFor}
import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._
import matryoshka.data.cofree._
import io.circe.parser.parse
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.util.Utf8
import scalaz.{Functor, Cofree}
import ujson.{Js, read}

import scala.collection.JavaConverters._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.util.prism._
import com.radix.shared.util.prism.implicits._
import squants.space.{Millilitres, Volume}

import scala.collection.JavaConverters._
import java.util

import com.radix.shared.util.prism.rpc.PrismProtocol.{PrismMetadata, PrismWithMetadata}
object derivations {
  implicit val fieldMapper: FieldMapper = DefaultFieldMapper

  implicit def mapSchemaForWellID[V](
      implicit schemaFor: SchemaFor[V]): SchemaFor[Map[WellID, V]] = {
    new SchemaFor[Map[WellID, V]] {
      override def schema(fieldMapper: FieldMapper): Schema =
        SchemaBuilder.map().values(schemaFor.schema(fieldMapper))
    }
  }
  implicit def mapDecoderWellID[T](
      implicit valueDecoder: Decoder[T]): Decoder[Map[WellID, T]] =
    new Decoder[Map[WellID, T]] {

      override def decode(value: Any,
                          schema: Schema,
                          fieldMapper: FieldMapper): Map[WellID, T] =
        value match {
          case map: java.util.Map[_, _] =>
            map.asScala.toMap.map {
              case (k, v) => WellID(
                implicitly[Decoder[String]]
                  .decode(k, schema, fieldMapper)) -> valueDecoder.decode(
                  v,
                  schema.getValueType,
                  fieldMapper)
            }
          case other => sys.error("Unsupported map " + other)
        }
    }
  implicit def mapEncoderWellID[V](
      implicit encoder: Encoder[V]): Encoder[Map[WellID, V]] =
    new Encoder[Map[WellID, V]] {

      override def encode(
          map: Map[WellID, V],
          schema: Schema,
          fieldMapper: FieldMapper): java.util.Map[String, AnyRef] = {
        require(schema != null)
        val java = new util.HashMap[String, AnyRef]
        map.foreach {
          case (k, v) =>
            java.put(k.id, encoder.encode(v, schema.getValueType, fieldMapper))
        }
        java
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
  implicit def SchemaForFix[F[_]: Functor](
      implicit ev: SchemaFor[F[REPLACE.type]]): SchemaFor[Fix[F]] = {
    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName =
      REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")
    val in = SchemaFor[F[REPLACE.type]].schema(fieldMapper).toString

    val json = parse(in).toOption.get.asArray.get
    val names = json.map(_.hcursor.downField("name").as[String].toOption.get)
    val namespaces =
      json.map(_.hcursor.downField("namespace").as[String].toOption.get)

    //One of the forms REPLACE can take, since it can get serialized as a case class by avro4s for some reason when it's in a list-like type
    val replaceStr =
      s"""{"type":"record","name":"$replaceName","namespace":"$replaceNamespace","fields":[]}""".stripMargin
    //The straightforward type replace
    val replaceStr2 =
      s""""$replaceNamespace.$replaceName"""".stripMargin
    val replacement = namespaces
      .zip(names)
      .map(x => s"${x._1}.${x._2}")
      .mkString("[\"", "\",\"", "\"]")

    val lastFullReplaced =
      json.last.noSpaces
        .replaceAllLiterally(replaceStr, replacement)
        .replaceAllLiterally(replaceStr2, replacement)
    val lastName = "\"" + namespaces.last + "." + names.last + "\""
    val result = json.init
      .foldRight((lastFullReplaced, lastName))({
        case (json, (accumSchema, lastname)) =>
          (json.noSpaces
             .replaceAllLiterally(replaceStr, replacement)
             .replaceAllLiterally(replaceStr2, replacement)
             .replaceFirst(lastname, accumSchema),
           "\"" + json.hcursor
             .downField("namespace")
             .as[String]
             .toOption
             .get + "." + json.hcursor
             .downField("name")
             .as[String]
             .toOption
             .get + "\"")
      })
      ._1
    val toplevelunion = "[" + result + ",\"" + namespaces.tail
      .zip(names.tail)
      .map(x => s"${x._1}.${x._2}")
      .mkString("", "\",\"", "\"]")
    val resultSchema =
    new SchemaFor[Fix[F]] {
      override def schema(fieldMapper: FieldMapper): Schema =
        new Schema.Parser()
          .setValidate(true)
          .parse(toplevelunion.replaceAllLiterally(s"__$replaceName", ""))
    }
    resultSchema
  }

  case class CofreeTuple[F[_]: Functor, A, B](head: A, tail: F[B])
//  val x  = SchemaFor[CofreeTuple[F,A]]

  implicit def SchemaForCofree[F[_]: Functor, A](
                                            implicit ev1: SchemaFor[F[REPLACE.type]], ev2: SchemaFor[A],
                                            ev3: SchemaFor[CofreeTuple[F, A, REPLACE.type ]]): SchemaFor[Cofree[F, A]] = {

    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName =
      REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")

    val replaceStr =
      s"""{"type":"record","name":"$replaceName","namespace":"$replaceNamespace","fields":[]}""".stripMargin
    //The straightforward type replace
    val replaceStr2 =
      s""""$replaceNamespace.$replaceName"""".stripMargin

    val tuplSchema = SchemaFor[CofreeTuple[F, A, REPLACE.type]].schema(fieldMapper)
    val tuplName = tuplSchema.getFullName
    val replacedTuplName = s""" "$tuplName" """
    val tupl = tuplSchema.toString

    val replacedSchema = tupl.replaceAllLiterally(replaceStr, replacedTuplName).replaceAllLiterally(replaceStr2, replacedTuplName).replaceAllLiterally(s"__$replaceName", "")

    val resultSchema =
    new SchemaFor[Cofree[F, A]] {
      override def schema(fieldMapper: FieldMapper): Schema =
        new Schema.Parser()
          .setValidate(true)
          .parse(replacedSchema.replaceAllLiterally(s"__$replaceName", "").replaceAllLiterally(s"_$replaceName", "").replaceAllLiterally("CofreeTuple", "Cofree"))
    }
    resultSchema
  }

  /**
    * There's probably a much nicer way to do this given labelledGeneric. TODO explore this to generalize
    *
    * @param ev
    * @return
    */
  import shims._
  implicit def EncoderForCofree(
      implicit ev: SchemaFor[PrismWithMetadata]): Encoder[PrismWithMetadata] with Decoder[PrismWithMetadata] =
    new Encoder[PrismWithMetadata] with Decoder[PrismWithMetadata] {
      val arraySchema = Schema.createArray(ev.schema(fieldMapper))

      //TODO implement head and tails
      override def encode(t: PrismWithMetadata, schema: Schema, fieldMapper: FieldMapper): AnyRef = {
        val alg: Algebra[matryoshka.patterns.EnvT[Map[String, String], Container, *], GenericData.Record] = x => {
          val unpacked: (Map[String, String], Container[GenericData.Record]) = x.run
          val metadata: Map[String, String] = unpacked._1
          val containerOfRecord: Container[AnyRef] = unpacked._2.asInstanceOf[Container[AnyRef]]

          val record = new GenericData.Record(ev.schema(fieldMapper))
          val arraySchema = Schema.createArray(ev.schema(fieldMapper))
          val tailSchema = record.getSchema.getField("tail").schema

                            val tailRecordSchema = tailSchema
                              .getTypes
                              .get(tailSchema.getIndexNamed(containerOfRecord.getClass.getName))

                            val tailRecord = new GenericData.Record(tailRecordSchema)
                            val containsr =
                              new GenericData.Array[AnyRef](arraySchema, containerOfRecord.contains.asJava)

                            tailRecord.put("offset",
                              Encoder[Offset].encode(containerOfRecord.offset,
                                SchemaFor[Offset].schema(fieldMapper),
                                fieldMapper))
                            tailRecord.put("uuid",
                              Encoder[UUID].encode(containerOfRecord.uuid,
                                SchemaFor[UUID].schema(fieldMapper),
                                fieldMapper))
                            tailRecord.put("contains", containsr)
                            containerOfRecord match {
                              case Plate(_, props, _, _) => {
                                tailRecord.put(
                                  "props",
                                  Encoder[PlatePropertiesUnits]
                                    .encode(props, AvroSchema[PlatePropertiesUnits], fieldMapper))
                              }
                              case Well(_, id, props, _, _) => {
                                tailRecord.put(
                                  "id",
                                  Encoder[WellID].encode(id,
                                    SchemaFor[WellID].schema(fieldMapper),
                                    fieldMapper))
                                tailRecord.put(
                                  "props",
                                  Encoder[WellPropertiesUnits]
                                    .encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
                              }
                              case HeatableWell(_, id, props, tempProps, _, _) => {
                                tailRecord.put(
                                  "id",
                                  Encoder[WellID].encode(id,
                                    SchemaFor[WellID].schema(fieldMapper),
                                    fieldMapper))
                                tailRecord.put(
                                  "props",
                                  Encoder[WellPropertiesUnits]
                                    .encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
                                tailRecord.put(
                                  "tempProps",
                                  Encoder[WellTempProperties].encode(tempProps,
                                    AvroSchema[WellTempProperties],
                                    fieldMapper))
                              }
                              case Fluid(_, volume, _, _) => {
                                tailRecord.put(
                                  "volume",
                                  Encoder[Volume].encode(volume, AvroSchema[Volume], fieldMapper))
                              }
                              case PipetteTip(_, id, props, _, _) => {
                                tailRecord.put("id", Encoder[WellID].encode(id, SchemaFor[WellID].schema(fieldMapper), fieldMapper))
                                tailRecord.put("props", Encoder[PipetteTipData].encode(props, SchemaFor[PipetteTipData].schema(fieldMapper), fieldMapper))
                              }


            case _ => Unit
          }
          record.put("tail", tailRecord)
          record.put("head", Encoder[PrismMetadata].encode(metadata, AvroSchema[PrismMetadata], fieldMapper))
          record

        }
        val R = implicitly[Recursive.Aux[PrismWithMetadata, matryoshka.patterns.EnvT[PrismMetadata, Container, ?]]]

        R.cata[GenericData.Record](t)(alg)
      }

      //TODO implement head and tails
      override def decode(value: Any, schema: Schema, fieldMapper: FieldMapper): PrismWithMetadata = {
        val genRecord = value.asInstanceOf[GenericRecord]

        val getMeta = (record: GenericRecord) => {
          Decoder[PrismMetadata].decode(record.get("head"), AvroSchema[PrismMetadata], fieldMapper)
        }

        val getContainer = (record: GenericRecord) => {
          val tailRecord = record.get("tail").asInstanceOf[GenericRecord]

          val uuid = Decoder[UUID].decode(tailRecord.get("uuid"),
            AvroSchema[UUID],
            fieldMapper)
          val offset = Decoder[Offset].decode(tailRecord.get("offset"),
            AvroSchema[Offset],
            fieldMapper)
          val contains = tailRecord
            .get("contains")
            .asInstanceOf[java.util.Collection[GenericRecord]]
            .asScala
            .toList

          val tail =
          tailRecord.getSchema.getName match {
            case "Fluid" => {
              val vol = Decoder[Volume].decode(tailRecord.get("volume"),
                AvroSchema[Volume],
                fieldMapper)
              new Fluid[GenericRecord](offset, volume = vol, contains, uuid)
            }
            case "Well" => {
              val wid = Decoder[WellID].decode(tailRecord.get("id"),
                AvroSchema[WellID],
                fieldMapper)
              val props = Decoder[WellPropertiesUnits].decode(
                tailRecord.get("props"),
                AvroSchema[WellPropertiesUnits],
                fieldMapper)
              new Well[GenericRecord](offset, wid, props, contains, uuid)
            }
            case "HeatableWell" => {
              val wid = Decoder[WellID].decode(tailRecord.get("id"),
                AvroSchema[WellID],
                fieldMapper)
              val props = Decoder[WellPropertiesUnits].decode(
                tailRecord.get("props"),
                AvroSchema[WellPropertiesUnits],
                fieldMapper)
              val tempProps =
                Decoder[WellTempProperties].decode(
                  tailRecord.get("tempProps"),
                  AvroSchema[WellTempProperties],
                  fieldMapper)
              new HeatableWell[GenericRecord](offset,
                wid,
                props,
                tempProps,
                contains,
                uuid)
            }
            case "Plate" => {
              val props =
                Decoder[PlatePropertiesUnits].decode(
                  tailRecord.get("props"),
                  AvroSchema[PlatePropertiesUnits],
                  fieldMapper)
              new Plate[GenericRecord](offset, props, contains, uuid)
            }
            case "Robot" => new Robot[GenericRecord](offset, contains, uuid)
            case "World" => new World[GenericRecord](offset, contains, uuid)
            case "Shim" => new Shim[GenericRecord](offset, contains, uuid)
            case "PipetteTip" => {
              val wid = Decoder[WellID].decode(tailRecord.get("id"),
                AvroSchema[WellID],
                fieldMapper)
              val props = Decoder[PipetteTipData].decode(
                tailRecord.get("props"),
                AvroSchema[PipetteTipData],
                fieldMapper)
              new PipetteTip[GenericRecord](offset, wid, props, contains, uuid)
            }
          }
          tail
        }
        def unwrap(gen: GenericRecord): (PrismMetadata, Container[GenericRecord]) = {
          (getMeta(gen), getContainer(gen))
        }
        scalaz.Cofree.unfold[Container, PrismMetadata, GenericRecord](genRecord)(unwrap)
      }
    }

  /**
    * There's probably a much nicer way to do this given labelledGeneric. TODO explore this to generalize
    *
    * @param ev
    * @return
    */
  implicit def EncoderForContainer(implicit ev: SchemaFor[Fix[Container]])
    : Encoder[Fix[Container]] with Decoder[Fix[Container]] =
    new Encoder[Fix[Container]] with Decoder[Fix[Container]] {
      private[this] val arraySchema = Schema.createArray(ev.schema(fieldMapper))
      private[this] val encoderAlg: Algebra[Container, AnyRef] = cont => {
        val recordSchema = ev
          .schema(fieldMapper)
          .getTypes
          .get(ev.schema(fieldMapper).getIndexNamed(cont.getClass.getName))
        val record = new GenericData.Record(recordSchema)
        val containsr =
          new GenericData.Array[AnyRef](arraySchema, cont.contains.asJava)
        record.put("offset",
                   Encoder[Offset].encode(cont.offset,
                                          SchemaFor[Offset].schema(fieldMapper),
                                          fieldMapper))
        record.put("uuid",
                   Encoder[UUID].encode(cont.uuid,
                                        SchemaFor[UUID].schema(fieldMapper),
                                        fieldMapper))
        record.put("contains", containsr)
        cont match {
          case Plate(_, props, _, _) => {
            record.put(
              "props",
              Encoder[PlatePropertiesUnits]
                .encode(props, AvroSchema[PlatePropertiesUnits], fieldMapper))
          }
          case Well(_, id, props, _, _) => {
            record.put(
              "id",
              Encoder[WellID].encode(id,
                                     SchemaFor[WellID].schema(fieldMapper),
                                     fieldMapper))
            record.put(
              "props",
              Encoder[WellPropertiesUnits]
                .encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
          }
          case HeatableWell(_, id, props, tempProps, _, _) => {
            record.put(
              "id",
              Encoder[WellID].encode(id,
                                     SchemaFor[WellID].schema(fieldMapper),
                                     fieldMapper))
            record.put(
              "props",
              Encoder[WellPropertiesUnits]
                .encode(props, AvroSchema[WellPropertiesUnits], fieldMapper))
            record.put(
              "tempProps",
              Encoder[WellTempProperties].encode(tempProps,
                                                 AvroSchema[WellTempProperties],
                                                 fieldMapper))
          }
          case Fluid(_, volume, _, _) => {
            record.put(
              "volume",
              Encoder[Volume].encode(volume, AvroSchema[Volume], fieldMapper))

          }
          case PipetteTip(_, id, props, _, _) => {
            record.put(
              "id",
              Encoder[WellID].encode(id,
                SchemaFor[WellID].schema(fieldMapper),
                fieldMapper))
            record.put(
              "props",
              Encoder[PipetteTipData]
                .encode(props, AvroSchema[PipetteTipData], fieldMapper))
          }
          case _ => Unit
        }
        record
      }

      override def encode(t: Fix[Container],
                          schema: Schema,
                          fieldMapper: FieldMapper): AnyRef = t.cata(encoderAlg)

      private[this] val decoderCoalgebra: Coalgebra[Container, GenericRecord] =
        record => {
          val uuid = Decoder[UUID].decode(record.get("uuid"),
                                          AvroSchema[UUID],
                                          fieldMapper)
          val offset = Decoder[Offset].decode(record.get("offset"),
                                              AvroSchema[Offset],
                                              fieldMapper)
          val contains = record
            .get("contains")
            .asInstanceOf[java.util.Collection[GenericRecord]]
            .asScala
            .toList
          record.getSchema.getName match {
            case "Fluid" => {
              val vol = Decoder[Volume].decode(record.get("volume"),
                                               AvroSchema[Volume],
                                               fieldMapper)
              new Fluid[GenericRecord](offset, volume = vol, contains, uuid)
            }
            case "Well" => {
              val wid = Decoder[WellID].decode(record.get("id"),
                                               AvroSchema[WellID],
                                               fieldMapper)
              val props = Decoder[WellPropertiesUnits].decode(
                record.get("props"),
                AvroSchema[WellPropertiesUnits],
                fieldMapper)
              new Well[GenericRecord](offset, wid, props, contains, uuid)
            }
            case "HeatableWell" => {
              val wid = Decoder[WellID].decode(record.get("id"),
                                               AvroSchema[WellID],
                                               fieldMapper)
              val props = Decoder[WellPropertiesUnits].decode(
                record.get("props"),
                AvroSchema[WellPropertiesUnits],
                fieldMapper)
              val tempProps =
                Decoder[WellTempProperties].decode(
                  record.get("tempProps"),
                  AvroSchema[WellTempProperties],
                  fieldMapper)
              new HeatableWell[GenericRecord](offset,
                                              wid,
                                              props,
                                              tempProps,
                                              contains,
                                              uuid)
            }
            case "PipetteTip" => {
              val wid = Decoder[WellID].decode(record.get("id"),
                                               AvroSchema[WellID],
                                               fieldMapper)
              val props = Decoder[PipetteTipData].decode(
                record.get("props"),
                AvroSchema[PipetteTipData],
                fieldMapper)
              new PipetteTip[GenericRecord](offset, wid, props, contains, uuid)
          }
            case "Plate" => {
              val props =
                Decoder[PlatePropertiesUnits].decode(
                  record.get("props"),
                  AvroSchema[PlatePropertiesUnits],
                  fieldMapper)
              new Plate[GenericRecord](offset, props, contains, uuid)
            }
            case "Robot" => new Robot[GenericRecord](offset, contains, uuid)
            case "World" => new World[GenericRecord](offset, contains, uuid)
            case "Shim"  => new Shim[GenericRecord](offset, contains, uuid)
          }
        }

      override def decode(value: Any,
                          schema: Schema,
                          fieldMapper: FieldMapper): Fix[Container] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Container]](decoderCoalgebra)
      }
    }

  implicit def AvroPath(implicit ev: SchemaFor[Fix[Path]])
    : Encoder[Fix[Path]] with Decoder[Fix[Path]] = {
    new Encoder[Fix[Path]] with Decoder[Fix[Path]] {

      private[this] val arraySchema = Schema.createArray(ev.schema(fieldMapper))
      private[this] val encoderAlg: Algebra[Path, AnyRef] = path => {
        val recordSchema = ev
          .schema(fieldMapper)
          .getTypes
          .get(ev.schema(fieldMapper).getIndexNamed(path.getClass.getName))
        val record = new GenericData.Record(recordSchema)
        path match {
          case Take(which, next) =>
            record.put("which",
                       Encoder[Fix[Container]].encode(
                         which,
                         SchemaFor[Fix[Container]].schema(fieldMapper),
                         fieldMapper))
            record.put("next", next)

          case Terminal(next) =>
            record.put("next",
                       Encoder[Fix[Container]].encode(
                         next,
                         SchemaFor[Fix[Container]].schema(fieldMapper),
                         fieldMapper))
          case _ => Unit
        }
        record
      }

      private[this] val decoderCoalgebra: Coalgebra[Path, GenericRecord] =
        record => {

          record.getSchema.getName match {
            case "Take" => {
              val next = record.get("next").asInstanceOf[GenericRecord]
              val which = Decoder[Fix[Container]].decode(
                record.get("which"),
                AvroSchema[Fix[Container]],
                fieldMapper)
              new Take[GenericRecord](which, next)
            }
            case "Terminal" => {
              val next = Decoder[Fix[Container]].decode(
                record.get("next"),
                AvroSchema[Fix[Container]],
                fieldMapper)
              new Terminal[GenericRecord](next)
            }
          }
        }

      override def decode(value: Any,
                          schema: Schema,
                          fieldMapper: FieldMapper): Fix[Path] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Path]](decoderCoalgebra)
      }

      override def encode(t: Fix[Path],
                          schema: Schema,
                          fieldMapper: FieldMapper): AnyRef = t.cata(encoderAlg)
    }
  }

}
object MainTest extends App {
  import derivations._
  val emptyMetadata: PrismMetadata = Map()

  val simple: PrismWithMetadata = FixToCofreeMap(Shim(Seq(Shim(Seq.empty[Fix[Container]]).embed)).embed)
  val fluid1: PrismWithMetadata = FixToCofreeMap(Fluid[Fix[Container]](Millilitres(7)).embed)

  val well: PrismWithMetadata = Cofree(Map.empty, Well(Offset(), Seq(fluid1), WellID("foo"), WellProperties(0, 0, 0, 0, 0, 0).withUnits()))
  val complex: PrismWithMetadata = Cofree(Map.empty, Shim(Seq(Cofree(Map.empty, Shim(Seq(Cofree(Map.empty, Robot(
    Offset(),
    Seq(well)))))))))
  val complex2: PrismWithMetadata = Cofree(Map.empty[String, String], Robot[PrismWithMetadata](Offset(),
    Seq(
      Cofree(Map.empty[String, String], Robot[PrismWithMetadata](Offset(), Seq[PrismWithMetadata](Cofree(Map.empty[String, String], Robot(
        Offset(),
        Seq[PrismWithMetadata](Cofree(Map.empty[String, String], Plate[PrismWithMetadata](
          Offset(),
          PlatePropertiesUnits(Some(OriginOffset(10, 100).withUnits()),
            Map(WellID("biz") -> WellProperties(2, 3, 4, 5, 6, 7).withUnits())),
          Seq[PrismWithMetadata](complex)
        ))
      )))))))))
  println(SchemaFor[Fix[Container]].schema(DefaultFieldMapper))

  println(SchemaFor[PrismWithMetadata].schema(DefaultFieldMapper))
  val encoded = Encoder[PrismWithMetadata].encode(complex2, SchemaFor[PrismWithMetadata].schema(DefaultFieldMapper), DefaultFieldMapper)
  println(encoded)
  val decoder = Decoder[PrismWithMetadata].decode(encoded, SchemaFor[PrismWithMetadata].schema(DefaultFieldMapper), DefaultFieldMapper)
  println(complex2)
  println(decoder)
}
