package com.radix.shared.persistence.serializations.utils.rainbow.test

import org.scalatest._
import com.radix.utils.rainbowuservice.RainbowActorProtocol
import com.radix.rainbow._
import com.radix.rainbow.URainbow.UTypes._
import com.radix.shared.persistence.serializations.utils.rainbow
import com.radix.shared.persistence.serializations.utils.rainbow.derivations._
import com.sksamuel.avro4s.{AvroSchema, Decoder, Encoder, ImmutableRecord, SchemaFor}
import com.radix.shared.persistence.serializations.squants.schemas._
import org.apache.avro.generic.{GenericData, GenericRecord, GenericRecordBuilder}
import cats.Eq

object shared {

  // forcibly use GenericRecord equals
  // (since ImmutableRecord which the default things encode
  // to is a subclass of GenericRecord)
  implicit object EqG extends Eq[GenericRecord] {
    override def eqv(x: GenericRecord, y: GenericRecord): Boolean = {
      if (x.getSchema != y.getSchema) {
        false
      } else {
        val xz = new GenericData().deepCopy(x.getSchema, x)
        val yz = new GenericData().deepCopy(y.getSchema, y)
        xz == yz
      }
    }
  }
  val R_e = for {
    r <- Right(Rainbow[ID, LabMeta[Metadata]])
    r <- Right(r.replaceRoot(("root", WorldWithMetadata(Offset(), Map("foo" -> "bar")))))
    r <- r.add(Some("root"), ("robot", RobotWithMetadata(Offset(squants.Meters(1)), Map("baz" -> "quxx"))))
  } yield r
  val R = R_e.toOption.get
  //  val R =
  val newplate = PlateWithMetadata(Offset(squants.Meters(2)), Map("buzz" -> "fuzz"))
  val insert = URainbow.Insert(newplate, "plate", Some("root"))
  val relocate = URainbow.Relocate("robot", Some("bar"))
  val remove = URainbow.Remove("robot")
  val replace = URainbow.Replace(Some("plate"), "robot", newplate)
}

class EquivalenceSpec extends FlatSpec {
  import shared._
  "The rainbow actor protocol general error" should "have an equivalent schema to gen" in {
    assert(
      rainbow.ManualSerializers.URainbowGeneralErrorS.schema == AvroSchema[RainbowActorProtocol.URainbowGeneralError]
    )
  }
  it should "encode and decode a message" in {
    val err = RainbowActorProtocol.URainbowGeneralError("foo")
    val enc = rainbow.ManualSerializers.URainbowGeneralErrorE.encode(err).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.URainbowGeneralErrorD.decode(enc)
    val genenc = Encoder[RainbowActorProtocol.URainbowGeneralError].encode(err).asInstanceOf[GenericRecord]
    val gendec = Decoder[RainbowActorProtocol.URainbowGeneralError].decode(genenc)
    assert(gendec == err)
    assert(dec == err)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }
  "The RainbowMetaSerialized" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.SerialRainbowS.schema == AvroSchema[RainbowMetaSerialized])
  }
  it should "encode and decode a rainbow" in {

    val serialRainbow = R.toSerial
    val enc = rainbow.ManualSerializers.SerialRainbowE.encode(serialRainbow).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.SerialRainbowD.decode(enc)
    val genenc = Encoder[RainbowMetaSerialized].encode(serialRainbow).asInstanceOf[GenericRecord]
    val gendec = Decoder[RainbowMetaSerialized].decode(genenc)
    assert(gendec == serialRainbow)
    assert(R == dec.toRainbow)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }
  "The Cas instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.CasS.schema == AvroSchema[URainbow.Cas])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val thisR = R.replace(Some("robot"), "plate", newplate).toOption.get
    val cas = URainbow.Cas(R.toSerial, thisR.toSerial)
    val enc = rainbow.ManualSerializers.CasE.encode(cas).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.CasD.decode(enc)
    val genenc = Encoder[URainbow.Cas].encode(cas).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.Cas].decode(genenc)
    assert(gendec == cas)
    assert(cas == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The Insert instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.InsertS.schema == AvroSchema[URainbow.Insert])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val enc = rainbow.ManualSerializers.InsertE.encode(insert).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.InsertD.decode(enc)
    val genenc = Encoder[URainbow.Insert].encode(insert).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.Insert].decode(genenc)
    assert(gendec == insert)
    assert(insert == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The Remove instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.RemoveS.schema == AvroSchema[URainbow.Remove])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val enc = rainbow.ManualSerializers.RemoveE.encode(remove).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.RemoveD.decode(enc)
    val genenc = Encoder[URainbow.Remove].encode(remove).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.Remove].decode(genenc)
    assert(gendec == remove)
    assert(remove == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The Relocate instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.RelocateS.schema == AvroSchema[URainbow.Relocate])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val enc = rainbow.ManualSerializers.RelocateE.encode(relocate).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.RelocateD.decode(enc)
    val genenc = Encoder[URainbow.Relocate].encode(relocate).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.Relocate].decode(genenc)
    assert(gendec == relocate)
    assert(relocate == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The Replace instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.ReplaceS.schema == AvroSchema[URainbow.Replace])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val enc = rainbow.ManualSerializers.ReplaceE.encode(replace).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.ReplaceD.decode(enc)
    val genenc = Encoder[URainbow.Replace].encode(replace).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.Replace].decode(genenc)
    assert(gendec == replace)
    assert(replace == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The MultiOp instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.MultiOpS.schema == AvroSchema[URainbow.MultiOp])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val multiOp =
      URainbow.MultiOp(removed = Seq(remove), relocate = Seq(relocate), insert = Seq(insert), replace = Seq(replace))
    val enc = rainbow.ManualSerializers.MultiOpE.encode(multiOp).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.MultiOpD.decode(enc)
    val genenc = Encoder[URainbow.MultiOp].encode(multiOp).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.MultiOp].decode(genenc)
    assert(gendec == multiOp)
    assert(multiOp == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The StringMeta instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.StringMetaS.schema == AvroSchema[URainbow.StringMeta])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val stringmeta = URainbow.StringMeta("foo")
    val enc = rainbow.ManualSerializers.StringMetaE.encode(stringmeta).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.StringMetaD.decode(enc)
    val genenc = Encoder[URainbow.StringMeta].encode(stringmeta).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.StringMeta].decode(genenc)
    assert(gendec == stringmeta)
    assert(stringmeta == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The InsertRainbow instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.InsertRainbowS.schema == AvroSchema[URainbow.InsertRainbow])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val insertR = URainbow.InsertRainbow(R.toSerial, Some("root"))
    val enc = rainbow.ManualSerializers.InsertRainbowE.encode(insertR).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.InsertRainbowD.decode(enc)
    val genenc = Encoder[URainbow.InsertRainbow].encode(insertR).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.InsertRainbow].decode(genenc)
    assert(gendec == insertR)
    assert(insertR == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The RemoveAndMakeChildrenRoot instruction" should "have an equivalent schema to gen" in {
    assert(
      rainbow.ManualSerializers.RemoveAndMakeChildrenRootS.schema == AvroSchema[URainbow.RemoveAndMakeChildrenRoot]
    )
  }

  it should "encode and decode an equivalent schema to gen" in {
    val R_op = URainbow.RemoveAndMakeChildrenRoot("foo")
    val enc = rainbow.ManualSerializers.RemoveAndMakeChildrenRootE.encode(R_op).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.RemoveAndMakeChildrenRootD.decode(enc)
    val genenc = Encoder[URainbow.RemoveAndMakeChildrenRoot].encode(R_op).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.RemoveAndMakeChildrenRoot].decode(genenc)
    assert(gendec == R_op)
    assert(R_op == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The ReplaceReverse instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.ReplaceReverseS.schema == AvroSchema[URainbow.ReplaceReverse])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val R_rev = URainbow.ReplaceReverse(Some("robot"), "robot", newplate, Some("root"))
    val enc = rainbow.ManualSerializers.ReplaceReverseE.encode(R_rev).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.ReplaceReverseD.decode(enc)
    val genenc = Encoder[URainbow.ReplaceReverse].encode(R_rev).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.ReplaceReverse].decode(genenc)
    assert(gendec == R_rev)
    assert(R_rev == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

  "The RemoveMultiple instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.RemoveMultipleS.schema == AvroSchema[URainbow.RemoveMultiple])
  }

  it should "encode and decode an equivalent schema to gen" in {
    val R_multi = URainbow.RemoveMultiple(Set("root", "robot"))
    val enc = rainbow.ManualSerializers.RemoveMultipleE.encode(R_multi).asInstanceOf[GenericRecord]
    val dec = rainbow.ManualSerializers.RemoveMultipleD.decode(enc)
    val genenc = Encoder[URainbow.RemoveMultiple].encode(R_multi).asInstanceOf[GenericRecord]
    val gendec = Decoder[URainbow.RemoveMultiple].decode(genenc)
    assert(gendec == R_multi)
    assert(R_multi == dec)
    assert(Eq.eqv(enc, genenc))
    assert(gendec == dec)
  }

}

class EquivalenceSpec1 extends FlatSpec {
  import shared._

  "The RainbowCommandAndReverse instruction" should "have an equivalent schema to gen" in {
    assert(rainbow.ManualSerializers.RainbowCommandAndReverseS.schema == AvroSchema[URainbow.RainbowCommandAndReverse])
  }

  //  it should "encode and decode an equivalent schema to gen" in {
  //    val R_car = URainbow.RainbowCommandAndReverse(R.toSerial, Some("root"))
  //    val enc = rainbow.ManualSerializers.RainbowCommandAndReverseE.encode(R_car).asInstanceOf[GenericRecord]
  //    val dec = rainbow.ManualSerializers.RainbowCommandAndReverseD.decode(enc)
  //    val genenc = Encoder[URainbow.RainbowCommandAndReverse].encode(R_car).asInstanceOf[GenericRecord]
  //    val gendec = Decoder[URainbow.RainbowCommandAndReverse].decode(genenc)
  //    assert(gendec == R_car)
  //    assert(R_car == dec)
  //    assert(Eq.eqv(enc, genenc))
  //    assert(gendec == dec)
  //  }

  "The RainbowHistoricalResponse instruction" should "have an equivalent schema to gen" in {
    assert(
      rainbow.ManualSerializers.RainbowHistoricalResponseS.schema == AvroSchema[URainbow.RainbowHistoricalResponse]
    )
  }

  //  it should "encode and decode an equivalent schema to gen" in {
  //    val R_rhr = URainbow.RainbowHistoricalResponse()
  //    val enc = rainbow.ManualSerializers.RainbowHistoricalResponseE.encode(R_rhr).asInstanceOf[GenericRecord]
  //    val dec = rainbow.ManualSerializers.RainbowHistoricalResponseD.decode(enc)
  //    val genenc = Encoder[URainbow.RainbowHistoricalResponse].encode(R_rhr).asInstanceOf[GenericRecord]
  //    val gendec = Decoder[URainbow.RainbowHistoricalResponse].decode(genenc)
  //    assert(gendec == R_rhr)
  //    assert(R_rhr == dec)
  //    assert(Eq.eqv(enc, genenc))
  //    assert(gendec == dec)
  //  }

  "The RainbowHistoryResponse instruction" should "have an equivalent schema to gen" in {
    assert(
      rainbow.ManualSerializers.RainbowHistoryResponseS.schema == AvroSchema[
        RainbowActorProtocol.RainbowHistoryResponse
      ]
    )
  }

  //  it should "encode and decode an equivalent schema to gen" in {
  //    val R_rhr = RainbowActorProtocol.RainbowHistoryResponse()
  //    val enc = rainbow.ManualSerializers.RainbowHistoryResponseE.encode(R_rhr).asInstanceOf[GenericRecord]
  //    val dec = rainbow.ManualSerializers.RainbowHistoryResponseD.decode(enc)
  //    val genenc = Encoder[RainbowActorProtocol.RainbowHistoryResponse].encode(R_rhr).asInstanceOf[GenericRecord]
  //    val gendec = Decoder[RainbowActorProtocol.RainbowHistoryResponse].decode(genenc)
  //    assert(gendec == R_rhr)
  //    assert(R_rhr == dec)
  //    assert(Eq.eqv(enc, genenc))
  //    assert(gendec == dec)
  //  }

  "The RainbowHistoryWithParentResponse instruction" should "have an equivalent schema to gen" in {
    assert(
      rainbow.ManualSerializers.RainbowHistoryWithParentResponseS.schema == AvroSchema[
        RainbowActorProtocol.RainbowHistoryWithParentResponse
      ]
    )
  }

  //  it should "encode and decode an equivalent schema to gen" in {
  //    val R_rhpr = RainbowActorProtocol.RainbowHistoryWithParentResponse(R.toSerial, Some("root"))
  //    val enc = rainbow.ManualSerializers.RainbowHistoryWithParentResponseE.encode(R_rhpr).asInstanceOf[GenericRecord]
  //    val dec = rainbow.ManualSerializers.RainbowHistoryWithParentResponseD.decode(enc)
  //    val genenc = Encoder[RainbowActorProtocol.RainbowHistoryWithParentResponse].encode(R_rhpr).asInstanceOf[GenericRecord]
  //    val gendec = Decoder[RainbowActorProtocol.RainbowHistoryWithParentResponse].decode(genenc)
  //    assert(gendec == R_rhpr)
  //    assert(R_rhpr == dec)
  //    assert(Eq.eqv(enc, genenc))
  //    assert(gendec == dec)
  //  }

}
