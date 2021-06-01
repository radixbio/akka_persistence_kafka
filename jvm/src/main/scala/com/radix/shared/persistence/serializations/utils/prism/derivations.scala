package com.radix.shared.persistence.serializations.utils.prism

import java.util.UUID

import com.radix.shared.defs.pipettingrobot.OpentronsPlateModel.{OriginOffset, OriginOffsetUnits, PipetteTipData, PlatePropertiesUnits, WellID, WellProperties, WellPropertiesUnits, WellsU}
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
import scalaz.{Cofree, Functor}
import ujson.{read, Js}

import scala.collection.JavaConverters._
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.util.prism._
import com.radix.shared.util.prism.implicits._
import squants.space.{Millilitres, Volume}

import scala.collection.JavaConverters._
import java.util

import akka.persistence.SnapshotMetadata
import com.radix.shared.defs.pipettingrobot.OpentronsPipetteModel.PipetteProperties
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.util.prism.rpc.PrismProtocol.{CASMetadataFailure, CASMetadataResponse, CASMetadataSuccess, CASTree, CASTreeAddMetadata, CASTreeFailure, CASTreeInitialMetadata, CASTreeInsert, CASTreeMergeFluid, CASTreeMetadata, CASTreeMove, CASTreeRemove, CASTreeResponse, CASTreeSplitFluid, CASTreeSuccess, Find, FindSuccess, GetLatestMetadata, GetLatestTree, GetTree, GetTreeNoUUIDFound, GetTreeResponse, GetTreeSuccess, NoFindResponse, PrismMetadata, PrismWithMetadata, Request}
import com.radix.shared.util.prism.rpc.PrismProtocol.{PrismMetadata, PrismWithMetadata}
import com.radix.utils.prism.PrismNoMeta
import shapeless.the
object derivations {
  implicit val fieldMapper: FieldMapper = DefaultFieldMapper

  implicit def mapSchemaForWellID[V](implicit schemaFor: SchemaFor[V]): SchemaFor[Map[WellID, V]] = {
    new SchemaFor[Map[WellID, V]] {
      override def schema: Schema =
        SchemaBuilder.map().values(schemaFor.schema)
      override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
    }

  }
  implicit def mapDecoderWellID[T](
    implicit valueDecoder: Decoder[T],
    schemaForImp: SchemaFor[T]
  ): Decoder[Map[WellID, T]] =
    new Decoder[Map[WellID, T]] {

      override def decode(value: Any): Map[WellID, T] =
        value match {
          case map: java.util.Map[_, _] =>
            map.asScala.toMap.map {
              case (k, v) =>
                WellID(
                  implicitly[Decoder[String]]
                    .decode(k)
                ) -> valueDecoder.decode(v)
            }
          case other => sys.error("Unsupported map " + other)
        }
      override def schemaFor: SchemaFor[Map[WellID, T]] = mapSchemaForWellID[T]

    }
  implicit def mapEncoderWellID[V](implicit encoder: Encoder[V], schemaForImp: SchemaFor[V]): Encoder[Map[WellID, V]] =
    new Encoder[Map[WellID, V]] {

      override def encode(
        map: Map[WellID, V]
      ): java.util.Map[String, AnyRef] = {
        require(schema != null)
        val java = new util.HashMap[String, AnyRef]
        map.foreach {
          case (k, v) =>
            java.put(k.id, encoder.encode(v))
        }
        java
      }
      override def schemaFor: SchemaFor[Map[WellID, V]] = mapSchemaForWellID[V]

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
  implicit def SchemaForFix[F[_]: Functor, Fx[_[_]]](
    implicit ev: SchemaFor[F[REPLACE.type]],
    lol: BirecursiveT[Fx]
  ): SchemaFor[Fx[F]] = {
    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName =
      REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")
    val in = SchemaFor[F[REPLACE.type]].schema.toString

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
          (
            json.noSpaces
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
              .get + "\""
          )
      })
      ._1
    val toplevelunion = "[" + result + ",\"" + namespaces.tail
      .zip(names.tail)
      .map(x => s"${x._1}.${x._2}")
      .mkString("", "\",\"", "\"]")
    val resultSchema =
      new SchemaFor[Fx[F]] {
        override def schema: Schema =
          new Schema.Parser()
            .setValidate(true)
            .parse(toplevelunion.replaceAllLiterally(s"__$replaceName", ""))
        override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
      }
    resultSchema
  }

  case class CofreeTuple[F[_]: Functor, A, B](head: A, tail: F[B])
//  val x  = SchemaFor[CofreeTuple[F,A]]

  implicit def SchemaForCofree[F[_]: Functor, A](
    implicit ev1: SchemaFor[F[REPLACE.type]],
    ev2: SchemaFor[A],
    ev3: SchemaFor[CofreeTuple[F, A, REPLACE.type]]
  ): SchemaFor[Cofree[F, A]] = {

    val replaceNamespace = REPLACE.getClass.getName.split('$').head
    val replaceName =
      REPLACE.getClass.getSimpleName.replaceAllLiterally("$", "")

    val replaceStr =
      s"""{"type":"record","name":"$replaceName","namespace":"$replaceNamespace","fields":[]}""".stripMargin
    //The straightforward type replace
    val replaceStr2 =
      s""""$replaceNamespace.$replaceName"""".stripMargin

    val tuplSchema = SchemaFor[CofreeTuple[F, A, REPLACE.type]].schema
    val tuplName = tuplSchema.getFullName
    val replacedTuplName = s""" "$tuplName" """
    val tupl = tuplSchema.toString

    val replacedSchema = tupl
      .replaceAllLiterally(replaceStr, replacedTuplName)
      .replaceAllLiterally(replaceStr2, replacedTuplName)
      .replaceAllLiterally(s"__$replaceName", "")

    val resultSchema =
      new SchemaFor[Cofree[F, A]] {
        override def schema: Schema =
          new Schema.Parser()
            .setValidate(true)
            .parse(
              replacedSchema
                .replaceAllLiterally(s"__$replaceName", "")
                .replaceAllLiterally(s"_$replaceName", "")
                .replaceAllLiterally("CofreeTuple", "Cofree")
            )
        override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
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
    implicit ev: SchemaFor[PrismWithMetadata]
  ): Encoder[PrismWithMetadata] =
    new Encoder[PrismWithMetadata] {
      val arraySchema = Schema.createArray(ev.schema)
      override def schemaFor: SchemaFor[PrismWithMetadata] = ev

      //TODO implement head and tails
      override def encode(t: PrismWithMetadata): AnyRef = {
        val alg: Algebra[matryoshka.patterns.EnvT[Map[String, String], Container, *], GenericData.Record] = x => {
          val unpacked: (Map[String, String], Container[GenericData.Record]) = x.run
          val metadata: Map[String, String] = unpacked._1
          val containerOfRecord: Container[AnyRef] = unpacked._2.asInstanceOf[Container[AnyRef]]

          val record = new GenericData.Record(ev.schema)
          val arraySchema = Schema.createArray(ev.schema)
          val tailSchema = record.getSchema.getField("tail").schema

          val tailRecordSchema = tailSchema.getTypes
            .get(tailSchema.getIndexNamed(containerOfRecord.getClass.getName))

          val tailRecord = new GenericData.Record(tailRecordSchema)
          val containsr =
            new GenericData.Array[AnyRef](arraySchema, containerOfRecord.contains.asJava)

          tailRecord.put(
            "offset",
            Encoder[Offset].encode(containerOfRecord.offset)
          )
          tailRecord.put(
            "uuid",
            Encoder[UUID].encode(containerOfRecord.uuid)
          )
          tailRecord.put("contains", containsr)
          containerOfRecord match {
            case Plate(_, props, _, _) => {
              tailRecord.put(
                "props",
                Encoder[PlatePropertiesUnits]
                  .encode(props)
              )
            }
            case Well(_, id, props, _, _) => {
              tailRecord.put("id", Encoder[WellID].encode(id))
              tailRecord.put(
                "props",
                Encoder[WellPropertiesUnits]
                  .encode(props)
              )
            }
            case HeatableWell(_, id, props, tempProps, _, _) => {
              tailRecord.put("id", Encoder[WellID].encode(id))
              tailRecord.put(
                "props",
                Encoder[WellPropertiesUnits]
                  .encode(props)
              )
              tailRecord.put(
                "tempProps",
                Encoder[WellTempProperties].encode(tempProps)
              )
            }
            case Fluid(_, volume, _, _) => {
              tailRecord.put("volume", Encoder[Volume].encode(volume))
            }
            case PipetteTip(_, id, props, _, _) => {
              tailRecord.put("id", Encoder[WellID].encode(id))
              tailRecord.put(
                "props",
                Encoder[PipetteTipData].encode(props)
              )
            }
            case Pipettor(props, tipOriginalOffset, _, _, _) => {
              tailRecord.put(
                "props",
                Encoder[PipetteProperties].encode(props)
              )
              tailRecord.put(
                "tipOriginalOffset",
                Encoder[Option[Offset]]
                  .encode(tipOriginalOffset)
              )
            }

            case _ => Unit
          }
          record.put("tail", tailRecord)
          record.put("head", Encoder[PrismMetadata].encode(metadata))
          record

        }
        val R = implicitly[Recursive.Aux[PrismWithMetadata, matryoshka.patterns.EnvT[PrismMetadata, Container, ?]]]

        R.cata[GenericData.Record](t)(alg)
      }
    }
  implicit def DecoderForCofree(implicit ev: SchemaFor[PrismWithMetadata]): Decoder[PrismWithMetadata] =
    new Decoder[PrismWithMetadata] {
      override def schemaFor: SchemaFor[PrismWithMetadata] = ev
      //TODO implement head and tails
      override def decode(value: Any): PrismWithMetadata = {
        val genRecord = value.asInstanceOf[GenericRecord]

        val getMeta = (record: GenericRecord) => {
          Decoder[PrismMetadata].decode(record.get("head"))
        }

        val getContainer = (record: GenericRecord) => {
          val tailRecord = record.get("tail").asInstanceOf[GenericRecord]

          val uuid = Decoder[UUID].decode(tailRecord.get("uuid"))
          val offset = Decoder[Offset].decode(tailRecord.get("offset"))
          val contains = tailRecord
            .get("contains")
            .asInstanceOf[java.util.Collection[GenericRecord]]
            .asScala
            .toList

          val tail =
            tailRecord.getSchema.getName match {
              case "Fluid" => {
                val vol = Decoder[Volume].decode(tailRecord.get("volume"))
                new Fluid[GenericRecord](offset, volume = vol, contains, uuid)
              }
              case "Well" => {
                val wid = Decoder[WellID].decode(tailRecord.get("id"))
                val props = Decoder[WellPropertiesUnits].decode(
                  tailRecord.get("props")
                )
                new Well[GenericRecord](offset, wid, props, contains, uuid)
              }
              case "HeatableWell" => {
                val wid = Decoder[WellID].decode(tailRecord.get("id"))
                val props = Decoder[WellPropertiesUnits].decode(
                  tailRecord.get("props")
                )
                val tempProps =
                  Decoder[WellTempProperties].decode(
                    tailRecord.get("tempProps")
                  )
                new HeatableWell[GenericRecord](offset, wid, props, tempProps, contains, uuid)
              }
              case "Plate" => {
                val props =
                  Decoder[PlatePropertiesUnits].decode(
                    tailRecord.get("props")
                  )
                new Plate[GenericRecord](offset, props, contains, uuid)
              }
              case "Robot" => new Robot[GenericRecord](offset, contains, uuid)
              case "World" => new World[GenericRecord](offset, contains, uuid)
              case "Shim"  => new Shim[GenericRecord](offset, contains, uuid)
              case "PipetteTip" => {
                val wid = Decoder[WellID].decode(tailRecord.get("id"))
                val props =
                  Decoder[PipetteTipData].decode(tailRecord.get("props"))
                new PipetteTip[GenericRecord](offset, wid, props, contains, uuid)
              }
              case "Pipettor" => {
                val props =
                  Decoder[PipetteProperties].decode(tailRecord.get("props"))
                val tipOriginalOffset = Decoder[Option[Offset]]
                  .decode(tailRecord.get("tipOriginalOffset"))
                new Pipettor[GenericRecord](props, tipOriginalOffset, offset, contains, uuid)
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
  implicit def EncoderForContainer(
    implicit ev: SchemaFor[Fix[Container]]
  ): Encoder[Fix[Container]] =
    new Encoder[Fix[Container]] {
      override def schemaFor: SchemaFor[Fix[Container]] = ev

      private[this] val arraySchema = Schema.createArray(ev.schema)
      private[this] val encoderAlg: Algebra[Container, AnyRef] = cont => {
        val recordSchema = ev.schema.getTypes
          .get(ev.schema.getIndexNamed(cont.getClass.getName))
        val record = new GenericData.Record(recordSchema)
        val containsr =
          new GenericData.Array[AnyRef](arraySchema, cont.contains.asJava)
        record.put("offset", Encoder[Offset].encode(cont.offset))
        record.put("uuid", Encoder[UUID].encode(cont.uuid))
        record.put("contains", containsr)
        cont match {
          case Plate(_, props, _, _) => {
            record.put(
              "props",
              Encoder[PlatePropertiesUnits]
                .encode(props)
            )
          }
          case Well(_, id, props, _, _) => {
            record.put("id", Encoder[WellID].encode(id))
            record.put(
              "props",
              Encoder[WellPropertiesUnits]
                .encode(props)
            )
          }
          case HeatableWell(_, id, props, tempProps, _, _) => {
            record.put("id", Encoder[WellID].encode(id))
            record.put(
              "props",
              Encoder[WellPropertiesUnits]
                .encode(props)
            )
            record.put(
              "tempProps",
              Encoder[WellTempProperties].encode(tempProps)
            )
          }
          case Fluid(_, volume, _, _) => {
            record.put("volume", Encoder[Volume].encode(volume))

          }
          case PipetteTip(_, id, props, _, _) => {
            record.put("id", Encoder[WellID].encode(id))
            record.put(
              "props",
              Encoder[PipetteTipData]
                .encode(props)
            )
          }
          case Pipettor(props, tipOriginalOffset, _, _, _) => {
            record.put(
              "props",
              Encoder[PipetteProperties].encode(props)
            )
            record.put(
              "tipOriginalOffset",
              Encoder[Option[Offset]]
                .encode(tipOriginalOffset)
            )
          }
          case _ => Unit
        }
        record
      }

      override def encode(t: Fix[Container]): AnyRef = t.cata(encoderAlg)
    }

  implicit def DecoderForContainer(
    implicit ev: SchemaFor[Fix[Container]]
  ): Decoder[Fix[Container]] =
    new Decoder[Fix[Container]] {
      override def schemaFor: SchemaFor[Fix[Container]] = ev
      private[this] val decoderCoalgebra: Coalgebra[Container, GenericRecord] =
        record => {
          val uuid = Decoder[UUID].decode(record.get("uuid"))
          val offset = Decoder[Offset].decode(record.get("offset"))
          val contains = record
            .get("contains")
            .asInstanceOf[java.util.Collection[GenericRecord]]
            .asScala
            .toList
          record.getSchema.getName match {
            case "Fluid" => {
              val vol = Decoder[Volume].decode(record.get("volume"))
              new Fluid[GenericRecord](offset, volume = vol, contains, uuid)
            }
            case "Well" => {
              val wid = Decoder[WellID].decode(record.get("id"))
              val props =
                Decoder[WellPropertiesUnits].decode(record.get("props"))
              new Well[GenericRecord](offset, wid, props, contains, uuid)
            }
            case "HeatableWell" => {
              val wid = Decoder[WellID].decode(record.get("id"))
              val props =
                Decoder[WellPropertiesUnits].decode(record.get("props"))
              val tempProps =
                Decoder[WellTempProperties].decode(record.get("tempProps"))
              new HeatableWell[GenericRecord](offset, wid, props, tempProps, contains, uuid)
            }
            case "PipetteTip" => {
              val wid = Decoder[WellID].decode(record.get("id"))
              val props = Decoder[PipetteTipData].decode(record.get("props"))
              new PipetteTip[GenericRecord](offset, wid, props, contains, uuid)
            }
            case "Pipettor" => {
              val props =
                Decoder[PipetteProperties].decode(record.get("props"))
              val tipOriginalOffset =
                Decoder[Option[Offset]].decode(record.get("tipOriginalOffset"))
              new Pipettor[GenericRecord](props, tipOriginalOffset, offset, contains, uuid)
            }
            case "Plate" => {
              val props =
                Decoder[PlatePropertiesUnits].decode(record.get("props"))
              new Plate[GenericRecord](offset, props, contains, uuid)
            }
            case "Robot" => new Robot[GenericRecord](offset, contains, uuid)
            case "World" => new World[GenericRecord](offset, contains, uuid)
            case "Shim"  => new Shim[GenericRecord](offset, contains, uuid)
          }
        }

      override def decode(value: Any): Fix[Container] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Container]](decoderCoalgebra)
      }
    }

  implicit def AvroPathEncoder(implicit ev: SchemaFor[Fix[Path]]): Encoder[Fix[Path]] = {
    new Encoder[Fix[Path]] {
      override def schemaFor: SchemaFor[Fix[Path]] = ev

      private[this] val arraySchema = Schema.createArray(ev.schema)
      private[this] val encoderAlg: Algebra[Path, AnyRef] = path => {
        val recordSchema = ev.schema.getTypes
          .get(ev.schema.getIndexNamed(path.getClass.getName))
        val record = new GenericData.Record(recordSchema)
        path match {
          case Take(which, next) =>
            record.put(
              "which",
              Encoder[Fix[Container]].encode(which)
            )
            record.put("next", next)

          case Terminal(next) =>
            record.put(
              "next",
              Encoder[Fix[Container]].encode(next)
            )
          case _ => Unit
        }
        record
      }
      override def encode(t: Fix[Path]): AnyRef = t.cata(encoderAlg)
    }
  }
  implicit def AvroPathDecoder(implicit ev: SchemaFor[Fix[Path]]): Decoder[Fix[Path]] = {
    new Decoder[Fix[Path]] {
      override def schemaFor: SchemaFor[Fix[Path]] = ev

      private[this] val decoderCoalgebra: Coalgebra[Path, GenericRecord] =
        record => {

          record.getSchema.getName match {
            case "Take" => {
              val next = record.get("next").asInstanceOf[GenericRecord]
              val which = Decoder[Fix[Container]].decode(record.get("which"))
              new Take[GenericRecord](which, next)
            }
            case "Terminal" => {
              val next = Decoder[Fix[Container]].decode(record.get("next"))
              new Terminal[GenericRecord](next)
            }
          }
        }

      override def decode(value: Any): Fix[Path] = {
        val record = value.asInstanceOf[GenericRecord]
        record.ana[Fix[Path]](decoderCoalgebra)
      }

    }
  }

  implicit val schemaForPrismNoMeta: SchemaFor[PrismNoMeta] = new SchemaFor[PrismNoMeta] {
//    override def schema: Schema = the[SchemaFor[(Fix[Container], UUID)]].schema(fieldMapper)
    override def schema: Schema = Schema.create(Schema.Type.RECORD)
    override def fieldMapper: com.sksamuel.avro4s.FieldMapper = com.sksamuel.avro4s.DefaultFieldMapper
  }

  implicit val decoderForPrismNoMeta: Decoder[PrismNoMeta] = new Decoder[PrismNoMeta] {
    override def decode(value: Any): PrismNoMeta = {
      val (prism, uuid) = implicitly[Decoder[(Fix[Container], UUID)]].decode(value)
      PrismNoMeta(prism, uuid)
    }
    override def schemaFor: SchemaFor[PrismNoMeta] = schemaForPrismNoMeta
  }

  implicit val encoderForPrismNoMeta: Encoder[PrismNoMeta] = new Encoder[PrismNoMeta] {
    override def encode(t: PrismNoMeta): AnyRef = {
      val prismData = (t.prism, t.uuid)
      implicitly[Encoder[(Fix[Container], UUID)]].encode(prismData)
    }
    override def schemaFor: SchemaFor[PrismNoMeta] = schemaForPrismNoMeta
  }

}
object MainTest extends App {
  import derivations._
  val emptyMetadata: PrismMetadata = Map()

  val simple: PrismWithMetadata = FixToCofreeMap(Shim(Seq(Shim(Seq.empty[Fix[Container]]).embed)).embed)
  val fluid1: PrismWithMetadata = FixToCofreeMap(Fluid[Fix[Container]](Millilitres(7)).embed)

  val well: PrismWithMetadata =
    Cofree(Map.empty, Well(Offset(), Seq(fluid1), WellID("foo"), WellProperties(0, 0, 0, 0, 0, 0).withUnits()))
  val complex: PrismWithMetadata =
    Cofree(Map.empty, Shim(Seq(Cofree(Map.empty, Shim(Seq(Cofree(Map.empty, Robot(Offset(), Seq(well)))))))))
  val complex2: PrismWithMetadata = Cofree(
    Map.empty[String, String],
    Robot[PrismWithMetadata](
      Offset(),
      Seq(
        Cofree(
          Map.empty[String, String],
          Robot[PrismWithMetadata](
            Offset(),
            Seq[PrismWithMetadata](
              Cofree(
                Map.empty[String, String],
                Robot(
                  Offset(),
                  Seq[PrismWithMetadata](
                    Cofree(
                      Map.empty[String, String],
                      Plate[PrismWithMetadata](
                        Offset(),
                        PlatePropertiesUnits(
                          Some(OriginOffset(10, 100).withUnits()),
                          Map(WellID("biz") -> WellProperties(2, 3, 4, 5, 6, 7).withUnits())
                        ),
                        Seq[PrismWithMetadata](complex)
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )
  println(SchemaFor[Fix[Container]].schema)

  println(SchemaFor[PrismWithMetadata].schema)
  val encoded = Encoder[PrismWithMetadata].encode(
    complex2
  )
  println(encoded)
  val decoder = Decoder[PrismWithMetadata].decode(
    encoded
  )
  println(complex2)
  println(decoder)
}

object Serializers {

  import com.radix.shared.persistence.serializations.utils.prism.derivations._
  import com.radix.shared.persistence.serializations.squants.schemas._
  import org.apache.avro.{Schema, SchemaBuilder}
  import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper}
  import com.sksamuel.avro4s.SchemaFor

  import scala.collection.JavaConverters._

  class PrismNoMetaSerializer extends AvroSerializer[PrismNoMeta]

  class CASPersistAvroTree extends AvroSerializer[CASTree]

  class CASPersistAvroInsert extends AvroSerializer[CASTreeInsert]

  class CASPersistAvroMove extends AvroSerializer[CASTreeMove]

  class CASPersistAvroMergeFluid extends AvroSerializer[CASTreeMergeFluid]

  class CASPersistAvroSplitFluid extends AvroSerializer[CASTreeSplitFluid]

  class CASPersistAvroRemove extends AvroSerializer[CASTreeRemove]

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

      override def encode(
        map: Map[UUID, V]
      ): java.util.Map[String, AnyRef] = {
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

  class CASPersistMetadata extends AvroSerializer[CASTreeMetadata]

  class CASPersistAddMetadata extends AvroSerializer[CASTreeAddMetadata]

  class CASPersistInitialMetadata extends AvroSerializer[CASTreeInitialMetadata]

  class GetPersistLatestTree extends AvroSerializer[GetLatestTree]

  class GetPersistLatestMetadata extends AvroSerializer[GetLatestMetadata]

  class PersistFind extends AvroSerializer[Find]

  class PersistGetTree extends AvroSerializer[GetTree]

  class PersistFindSuccess extends AvroSerializer[FindSuccess]

  //TODO in derivations finish implementing the path serializer

  class PersistMetadataSuccess extends AvroSerializer[CASMetadataSuccess]

  class PersistNoFindResponse extends AvroSerializer[NoFindResponse]

  class PersistGetTreeSuccess extends AvroSerializer[GetTreeSuccess]

  class PersistGetTreeNoUUIDFound extends AvroSerializer[GetTreeNoUUIDFound]

  class PersistCasTreeSuccess extends AvroSerializer[CASTreeSuccess]

  class PersistCASTreeFailure extends AvroSerializer[CASTreeFailure]

  class PersistCASMetadataFailure extends AvroSerializer[CASMetadataFailure]

  class PersistGetTreeResponse extends AvroSerializer[GetTreeResponse]

  class PersistCASTreeResponse extends AvroSerializer[CASTreeResponse]

  class PersistCASMetadataResponse extends AvroSerializer[CASMetadataResponse]

  class PersistRequest extends AvroSerializer[Request]

  class PersistSnapshotMetadata extends AvroSerializer[SnapshotMetadata]

  //  implicit val requestEncoder = implicitly[Encoder[Request]]  //Fixes weird bug in prism historical serialization
  //  implicit val requestDecooder = implicitly[Decoder[Request]] //Fixes weird bug in prism historical serialization
  //  implicit val requestSchema = implicitly[SchemaFor[Request]] //Fixes weird bug in prism historical serialization

  // class PersistMetadataSuccess extends AvroSerializer[MetadataSuccess]

}
