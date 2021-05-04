package com.radix.shared.persistence.serializations.device_drivers.elemental.kafka_bridge

import java.time.Instant
import scala.util.{Failure, Success}
import scala.collection.JavaConverters._

import io.circe._

import com.radix.shared.persistence.AvroSerializer
import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.{Schema => ASchema}
import org.apache.avro.Schema.Type
import org.apache.kafka.connect.data.{SchemaBuilder, Struct, Schema => KCSchema}



object defns {
  private implicit def strToJson(str: String): Json = Json.fromString(str)

  case class SensorDetails(name: String, uuid: String, model: String, location: String) {
    def toStruct: Struct = new Struct(SensorDetails.kcSchema)
      .put("name", name)
      .put("uuid", uuid)
      .put("model", model)
      .put("location", location)
  }
  object SensorDetails {
    val schema = AvroSchema[SensorDetails]
    val kcSchema = convertSchema(schema)
  }

  sealed trait SensorRecord {
    val sensor: SensorDetails
    val battery: Int
    val gateway: String
    val rssi: Int
    val timestamp: Instant

    def schema: ASchema
    def kcSchema: KCSchema
    def toStruct: Struct = finalizeStruct(new Struct(kcSchema)
      .put("sensor", sensor.toStruct)
      .put("battery", battery)
      .put("gateway", gateway)
      .put("rssi", rssi)
      .put("timestamp", timestamp.getEpochSecond))

    protected def finalizeStruct(base:Struct): Struct
  }

  case class ElementA(
                       sensor: SensorDetails,
                       battery: Int,
                       gateway: String,
                       rssi: Int,
                       timestamp: Instant,
                       humidity: Float,
                       light: Float,
                       pressure: Float,
                       temperature: Float
                     ) extends SensorRecord {
    def schema: ASchema = ElementA.schema
    def kcSchema: KCSchema = ElementA.kcSchema

    override protected def finalizeStruct(base: Struct): Struct = base
      .put("humidity", humidity)
      .put("light", light)
      .put("pressure", pressure)
      .put("temperature", temperature)
  }
  object ElementA {
    val schema = AvroSchema[ElementA]
    val kcSchema = convertSchema(schema)
    val serializer = ElementAAvro()
  }

  case class ElementT(
                       sensor: SensorDetails,
                       battery: Int,
                       gateway: String,
                       rssi: Int,
                       timestamp: Instant,
                       temperature: Float,
                       pluggedIn: Boolean
                     ) extends SensorRecord {
    def schema: ASchema = ElementT.schema


    override def kcSchema: KCSchema = ElementT.kcSchema

    override protected def finalizeStruct(base: Struct): Struct = base
      .put("pluggedIn", pluggedIn)
      .put("temperature", temperature)
  }
  object ElementT {
    val schema = AvroSchema[ElementT]
    val kcSchema = convertSchema(schema)
    val serializer = ElementTAvro()
  }

  def decodeElementA(sensor: SensorDetails,
                     battery: Int,
                     gateway: String,
                     rssi: Int,
                     timestamp: Long,
                     cursor: ACursor): Decoder.Result[ElementA] = {
    for {
      humid <- cursor.downField("humidity").as[Float]
      light <- cursor.downField("light").as[Float]
      pressure <- cursor.downField("pressure").as[Float]
      temp <- cursor.downField("temperature").as[Float]
    } yield ElementA(sensor, battery, gateway, rssi, Instant.ofEpochSecond(timestamp), humid, light, pressure, temp)
  }

  def decodeElementT(sensor: SensorDetails,
                     battery: Int,
                     gateway: String,
                     rssi: Int,
                     timestamp: Long,
                     cursor: ACursor): Decoder.Result[ElementT] = {
    Right(ElementT(sensor, battery, gateway, rssi, Instant.ofEpochSecond(timestamp),
      cursor.downField("temperature").as[Float].getOrElse(-1f),
      cursor.downField("thermocouple_unplugged").as[Boolean].map(!_).getOrElse(true)))
  }

  implicit def DetailsDecoder: Decoder[SensorDetails] =
    (c: HCursor) => for {
      name <- c.downField("name").as[String]
      uuid <- c.downField("uuid").as[String]
      loc <- c.downField("location").as[String]
      model <- c.downField("machine").downField("model").as[String]
    } yield SensorDetails(name, uuid, model, loc)

  implicit def RecordDecoder(implicit sensor: SensorDetails): Decoder[SensorRecord] =
    (c: HCursor) => for {
      battery <- c.downField("battery").as[Int]
      gateway <- c.downField("gateway").as[String]
      rssi <- c.downField("rssi").as[Int]
      timestamp <- c.downField("sample_epoch").as[Long]
      record <- sensor.model match {
        case "element_a" => decodeElementA(sensor, battery, gateway, rssi, timestamp, c)
        case "element_t" => decodeElementT(sensor, battery, gateway, rssi, timestamp, c)
        case m => Left(DecodingFailure("Unknown sensor model "+m, List.empty))
      }
    } yield record

  case class SensorDetailsAvro() extends AvroSerializer[SensorDetails]
  case class ElementAAvro() extends AvroSerializer[ElementA]
  case class ElementTAvro() extends AvroSerializer[ElementT]

  def asRecords(in: Json)(implicit sensor: SensorDetails): List[SensorRecord] = {
    in.asArray.getOrElse(Vector.empty).toList.flatMap { json =>
      json.as[SensorRecord].toOption }
  }

  def convertSchema(raw:ASchema): KCSchema = {
    import ASchema.Type

    raw.getType match {
      case Type.BOOLEAN => KCSchema.BOOLEAN_SCHEMA
      case Type.DOUBLE => KCSchema.FLOAT64_SCHEMA
      case Type.FLOAT => KCSchema.FLOAT32_SCHEMA
      case Type.LONG => KCSchema.INT64_SCHEMA
      case Type.INT => KCSchema.INT32_SCHEMA
      case Type.STRING => KCSchema.STRING_SCHEMA
      case Type.BYTES | Type.FIXED => KCSchema.BYTES_SCHEMA
      case Type.NULL => KCSchema.STRING_SCHEMA
      case Type.ENUM => KCSchema.STRING_SCHEMA
      case Type.ARRAY => SchemaBuilder.array(convertSchema(raw.getElementType))
      case Type.MAP => SchemaBuilder.map(KCSchema.STRING_SCHEMA, convertSchema(raw.getValueType)).build()
      case Type.UNION =>
        val types = raw.getTypes
        if (types.size() == 1) {
          convertSchema(types.get(0))
        } else if (types.size() == 2 && (types.get(0).getType == Type.NULL ^ types.get(1).getType == Type.NULL)) {
          val nonNullType = if (types.get(0).getType == Type.NULL) types.get(0).getType else types.get(1).getType
          nonNullType match {
            case Type.BOOLEAN => KCSchema.OPTIONAL_BOOLEAN_SCHEMA
            case Type.DOUBLE => KCSchema.OPTIONAL_FLOAT64_SCHEMA
            case Type.FLOAT => KCSchema.OPTIONAL_FLOAT32_SCHEMA
            case Type.LONG => KCSchema.OPTIONAL_INT64_SCHEMA
            case Type.INT => KCSchema.OPTIONAL_INT32_SCHEMA
            case Type.STRING => KCSchema.OPTIONAL_STRING_SCHEMA
            case Type.BYTES | Type.FIXED => KCSchema.OPTIONAL_BYTES_SCHEMA
            case other => throw new IllegalArgumentException(s"Kafka connect does not support optional ${other.toString} types")
          }
        } else {
          throw new IllegalArgumentException(s"Kafka connect does not support union types")
        }
      case Type.RECORD =>
        val builder = SchemaBuilder.struct().name(raw.getFullName)
        raw.getFields.asScala.foldLeft(builder) { (build, field) =>
          build.field(field.name(), convertSchema(field.schema()))
        }.build()
    }
  }
}
