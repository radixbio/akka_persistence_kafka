package com.radix.shared.persistence.serializations.device_drivers.tecan

import logstage.IzLogger
import squants.motion.{Velocity, VolumeFlow}
import squants.space.{Microlitres, Millimeters, Volume}
import squants.time.{Milliseconds, Seconds, Time}

import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.device_drivers.tecan.defns.TecanCmds._
import com.radix.shared.persistence.serializations.squants._
import com.radix.shared.persistence.serializations.squants.schemas.{SchemaForLength, SchemaForTime, SchemaForVelocity, SchemaForVolume, SchemaForVolumeFlow, _}

object defns {
  val logger = IzLogger()

  object TecanCmds {
    sealed trait TipType
    case class EightChannel()
    case class NinetySixChannel()

    case class TecanFluidClass(fluidName: String)
    implicit def sToFC(fc: String): TecanFluidClass = TecanFluidClass(fc)

    case class TecanPlate(plateType: String, plateDispName: String)
    case class TecanDeck(deck: List[List[Option[TecanPlate]]]) {
      def coordsFromPlateName(name: String): (Int, Int) = {
        // format: off
        val index = for {
          row <- 0 to 2
          col <- 0 to 7
          if(deck(col)(row).getOrElse(TecanPlate("NO", "NO")).plateDispName == name)
        } yield (row, col+1)
        // format: on

        index.head
      }

      def plateFromName(name: String): TecanPlate = {
        val indexCanon = coordsFromPlateName(name)
        deck(indexCanon._1)(indexCanon._2 -1).get
      }

      def plateTypeFromName(name: String): String = {
        plateFromName(name).plateType
      }
    }

    def wellEncoding(rowNum: Int, maxRows: Int = 12, maxColumns: Int = 8): String = {
      //    8 is short; 12 is long
      if (maxColumns == 8 && maxRows == 12) {
        rowNum match {
          case 1  => return "0C08¯1000000000000"
          case 2  => return "0C080®300000000000"
          case 3  => return "0C0800¬70000000000"
          case 4  => return "0C08000¨?000000000"
          case 5  => return "0C080000 O00000000"
          case 6  => return "0C0800000\u0090o0000000"
          case 7  => return "0C08000000p¯000000"
          case 8  => return "0C0800000000¯10000"
          case 9  => return "0C08000000000®3000"
          case 10 => return "0C080000000000¬700"
          case 11 => return "0C0800000000000¨?0"
          case 12 => return "0C08000000000000 O"
          case _  => return "UnsupportedRowEncoding"
        }
      }
      "UnsupportedRowEncoding"
    }


    sealed trait TecanCommand {
      def mkLine: String
    }

    /**
     * Convert a string of 1s and 0s to a 10 base number. Shamelssly stolen from:
     * https://www.programmersought.com/article/87032881834/
     * @param src The input binary string. Can only contain 1s and 0s.
     * @return Intiger that was encoded by 1s and 0s.
     */
    def btod(src: String): Int = {
      if (!src.matches("[0|1]*")) {
        // This should never get hit with how we're calling this. But error handling is nice regardless.
        logger.error("Tip mask contained chars that weren't 1/0.")
        return 0
      }
      val tmp = src.reverse
      var res: Int = 0

      for (i <- 0 to tmp.length - 1) {
        res += tmp.substring(i, i + 1).toInt * (1 << i)
      }
      res
    }

    /**
     * Which tips to use are encoded in a binary number of 1s and 0s that then gets converted into a decimal number and included in the commands.
     * @param tipsToUse Sequence of booleans to use/not-use the tips.
     * @param maxNumTips Max tips to encode. Typically, this'll be 8 channels, but some Tecans can have as many as 12
     *                   channels. Numbers larger than 12 would be weird but not impossible. We have it default to 12 so
     *                   it typically does the right thing. If you only have 8 channels installed and send more, this
     *                   could cause errors, so correctly set this parameter for safety.
     * @return the tip mask conversion that's correct to include in the EVO commands.
     */
    def tipMask(tipsToUse: List[Boolean], maxNumTips: Int = 12): Int = {
      if (tipsToUse.length > maxNumTips) {
        logger.error(s"Too many tips provided for tip mask. For now we'll just use the first $maxNumTips.")
      }
      val tipInt = tipsToUse.take(maxNumTips).map(t => if (t) { "1" } else "0")
      btod(tipInt.mkString(""))
    }

    /**
     * Aspirate fluid with the Tecan's 8 channel pipet (canonically in TecanSpeak the "LiHa.")
     * Note that the Tecan doesn't have a single channel pipette. To do single channel pipetting, set the well encoding
     * to just be a single well and in the well encoding select one of the 8 tips (corresponding to the row # of the
     * well you want).
     * @param tipsToUse Which tips of the multichannel pipette to use.
     * @param aspVolumes Volumes to aspirate from each tip.
     * @param fluidClass The fluid class to think of with the fluid. This should typically be the largest quantity fluid
     *                   in the well.
     * @param plateN The name of the plate that you want to work with on the deck of the Tecan. Typically, this is the
     *               plate barcode.
     * @param rowNum The row that we want to aspirate from.
     * @param deck The tecan deck in question.
     * @param lihaNum which LiHa to use. If only one is installed (typically), this will be zero (the default). There
     *                can be up to two LiHas installed. So the value can be 0 or 1 depending on which you want to use.
     *                Keep in mind that you can't magically levitate fluid from one LiHa to another, so if you aspirate
     *                in one, you'll need to dispense from the same.
     */
    case class Aspirate(
                         tipsToUse: List[Boolean],
                         aspVolumes: List[Volume],
                         fluidClass: TecanFluidClass,
//                         fluidClass: String,
                         plateN: String,
                         rowNum: Int,
                         deck: TecanDeck,
                         lihaNum: Int,
                         tipSpacing: Int
                         )
      extends TecanCommand {
      override def mkLine: String = {
        val plateCoords = deck.coordsFromPlateName(plateN)
        val aspVolumeStr = aspVolumes.map(v => s""""${v.toMicrolitres}"""").mkString(",")
        s"""Aspirate(${tipMask(tipsToUse)},"${fluidClass.fluidName}",${aspVolumeStr},0,0,0,0,${(plateCoords._2+1) * 6},${plateCoords._1},1,
           |"${wellEncoding(rowNum, 12, 8)}",0,$lihaNum);""".stripMargin.replace("\n", "")
      }
    }


    object Aspirate{
      /**
       * Aspirate fluid with the Tecan's 8 channel pipet while pretending that it's a single channel pipette.
       * @param aspVolume The volume we want to aspirate from the well.
       * @param plateN The name of the plate that you want to work with on the deck of the Tecan. Typically, this is the
       *               plate barcode.
       * @param rowNum The row that we want to aspirate from.
       * @param colNum The column that we want to aspirate from.
       * @param fluidClass The fluid class to think of with the fluid. This should typically be the largest quantity fluid
       *                   in the well.
       * @param lihaNum which LiHa to use. If only one is installed (typically), this will be zero (the default). There
       *                can be up to two LiHas installed. So the value can be 0 or 1 depending on which you want to use.
       *                Keep in mind that you can't magically levitate fluid from one LiHa to another, so if you aspirate
       *                in one, you'll need to dispense from the same.
       * @param deck The tecan deck in question.
       */
      def apply(aspVolume: Volume,
                plateN: String,
                rowNum: Int,
                colNum: Int,
                fluidClass: Option[TecanFluidClass],
                lihaNum: Option[Int],
                tipSpacing: Option[Int])(implicit deck: TecanDeck): Aspirate = {
        val tipsToUse = (1 to 8).map{i => if(i==rowNum)true else false}.toList
        val aspVolumes = (1 to 8).map{i => if(i==rowNum)aspVolume else Microlitres(0)}.toList
        new Aspirate(tipsToUse, aspVolumes, fluidClass.getOrElse("Water free dispense"), plateN, rowNum, deck, lihaNum.getOrElse(0), tipSpacing.getOrElse(1))
      }

      def apply(tipsToUse: List[Boolean],
                aspVolumes: List[Volume],
                fluidClass: TecanFluidClass,
                plateN: String,
                rowNum: Int,
                lihaNum: Int = 0,
                tipSpacing: Int=1)(implicit deck: TecanDeck): Aspirate = {
        Aspirate(tipsToUse, aspVolumes, fluidClass, plateN, rowNum, deck, lihaNum, tipSpacing)
      }
    }


    /**
     * Dispense fluid with the Tecan's 8 channel pipet (canonically in TecanSpeak the "LiHa.")
     * Note that the Tecan doesn't have a single channel pipette. To do single channel pipetting, set the well encoding
     * to just be a single well and in the well encoding select one of the 8 tips (corresponding to the row # of the
     * well you want).
     * @param tipsToUse Which tips of the multichannel pipette to use.
     * @param dispVolumes Volumes to aspirate from each tip.
     * @param fluidClass The fluid class to think of with the fluid. This should typically be the largest quantity fluid
     *                   in the well.
     * @param plateN The name of the plate that you want to work with on the deck of the Tecan. Typically, this is the
     *               plate barcode.
     * @param rowNum The row that we want to aspirate from.
     * @param deck The tecan deck in question.
     * @param lihaNum which LiHa to use. If only one is installed (typically), this will be zero (the default). There
     *                can be up to two LiHas installed. So the value can be 0 or 1 depending on which you want to use.
     *                Keep in mind that you can't magically levitate fluid from one LiHa to another, so if you aspirate
     *                in one, you'll need to dispense from the same.
     */
    case class Dispense(
                         tipsToUse: List[Boolean],
                         dispVolumes: List[Volume],
                         fluidClass: TecanFluidClass,
                         plateN: String,
                         rowNum: Int,
                         deck: TecanDeck,
                         lihaNum: Int,
                         tipSpacing: Int)
      extends TecanCommand {
      override def mkLine: String = {
        val plateCoords = deck.coordsFromPlateName(plateN)
        val aspVolumeStr = dispVolumes.map(v => s""""${v.toMicrolitres}"""").mkString(",")
        s"""Dispense(${tipMask(tipsToUse)},"${fluidClass.fluidName}",${aspVolumeStr},0,0,0,0,${(plateCoords._2+1) * 6},${plateCoords._1},1,
           |"${wellEncoding(rowNum, 12, 8)}",0,$lihaNum);""".stripMargin.replace("\n", "")
      }
    }

    object Dispense{
      /**
       * Aspirate fluid with the Tecan's 8 channel pipet while pretending that it's a single channel pipette.
       * @param dispVolume The volume we want to dispirate from the well.
       * @param plateN The name of the plate that you want to work with on the deck of the Tecan. Typically, this is the
       *               plate barcode.
       * @param rowNum The row that we want to aspirate from.
       * @param colNum The column that we want to aspirate from.
       * @param fluidClass The fluid class to think of with the fluid. This should typically be the largest quantity fluid
       *                   in the well.
       * @param lihaNum which LiHa to use. If only one is installed (typically), this will be zero (the default). There
       *                can be up to two LiHas installed. So the value can be 0 or 1 depending on which you want to use.
       *                Keep in mind that you can't magically levitate fluid from one LiHa to another, so if you aspirate
       *                in one, you'll need to dispense from the same.
       * @param deck The tecan deck in question.
       */
      def apply(dispVolume: Volume,
                plateN: String,
                rowNum: Int,
                colNum: Int,
                fluidClass: Option[TecanFluidClass],
                lihaNum: Option[Int], tipSpacing: Option[Int])(implicit deck: TecanDeck): Dispense = {
        val tipsToUse = (1 to 8).map{i => if(i==rowNum)true else false}.toList
        val dispVolumes = (1 to 8).map{i => if(i==rowNum)dispVolume else Microlitres(0)}.toList
        new Dispense(tipsToUse, dispVolumes, fluidClass.getOrElse("Water free dispense"), plateN, rowNum, deck, lihaNum.getOrElse(0), tipSpacing.getOrElse(1))
      }

      def apply(tipsToUse: List[Boolean],
                dispVolumes: List[Volume],
                fluidClass: TecanFluidClass,
                plateN: String,
                rowNum: Int,
                lihaNum: Int=0,
                tipSpacing: Int=1)(implicit deck: TecanDeck): Dispense = {
        Dispense(tipsToUse, dispVolumes, fluidClass, plateN, rowNum, deck, lihaNum, tipSpacing)
      }
    }

    /**
     * Mix command to mix fluid in a well by pipetting up and down.
     *
     * @param tipsToUse Which tips to use. This is a list of up to 8 to 12 channels for if we want to use them or not.
     * @param mixVolumes How much working volume we want to mix with. Typically you want to keep this value below the
     *                   volume of fluid to prevent creating bubbles.
     * @param fluidClass The fluid class to use. This defaults to "Water free dispense," but if you're working with an
     *                   esoteric fluid, you probably want to set this to the actual fluid class.
     * @param plateN The plate name you want to aspirate from. Typically the barcode.
     * @param rowNum The well's row num in the plate.
     * @param numCycles How many cycles you want to mix for. If none, this defaults to 1 cycle.
     * @param lihaNum Which LiHa arm to use. Defaults to 0--the default arm that's always present. (Some Tecans can have
     *                two 8 channel LiHa heads).
     * @param tipSpacing Spacing for the tips.
     * @param deck Tecan deck layout. The well plate used in the command needs to be in the Tecan Deck.
     */
    case class Mix(
                    tipsToUse: List[Boolean],
                    mixVolumes: List[Volume],
                    fluidClass: TecanFluidClass,
                    plateN: String,
                    rowNum: Int,
                    numCycles: Int,
                    deck: TecanDeck,
                    lihaNum: Int,
                    tipSpacing: Int)
      extends TecanCommand {
      override def mkLine: String = {
        val plateCoords = deck.coordsFromPlateName(plateN)
        val aspVolumeStr = mixVolumes.map(v => s""""${v.toMicrolitres}"""").mkString(",")
        s"""Mix(${tipMask(tipsToUse)},"${fluidClass.fluidName}",${aspVolumeStr},0,0,0,0,${(plateCoords._2+1) * 6},${plateCoords._1},1,
           |"${wellEncoding(rowNum, 12, 8)}",$numCycles,0,$lihaNum);""".stripMargin.replace("\n", "")
      }
    }


    object Mix{
      def apply(aspVolume: Volume,
                plateN: String,
                rowNum: Int,
                colNum: Int,
                numCycles: Option[Int],
                fluidClass: Option[TecanFluidClass],
                lihaNum: Option[Int],
                tipSpacing: Option[Int])(implicit deck: TecanDeck): Mix = {
        val tipsToUse = (1 to 8).map{i => if(i==rowNum)true else false}.toList
        val mixVolumes = (1 to 8).map{i => if(i==rowNum)aspVolume else Microlitres(0)}.toList
        new Mix(tipsToUse,
          mixVolumes,
          fluidClass.getOrElse("Water free dispense"),
          plateN,
          rowNum,
          numCycles.getOrElse(1),
          deck,
          lihaNum.getOrElse(0),
          tipSpacing.getOrElse(1))
      }

      def apply(tipsToUse: List[Boolean],
                mixVolumes: List[Volume],
                fluidClass: TecanFluidClass,
                plateN: String,
                rowNum: Int,
                numCycles: Int,
                lihaNum: Int = 0,
                tipSpacing: Int = 1)(implicit deck: TecanDeck): Mix = {
        Mix(tipsToUse, mixVolumes, fluidClass, plateN, rowNum, numCycles, deck, lihaNum, tipSpacing)
      }
    }


    /**
     * Pick up pipette tips on the 8 channel pipette(s).
     * @param tipsToUse
     * @param plateN
     * @param rowNum
     * @param ditiEnum From the manual, "DITI index (see 9.4.5 “Labware Types and DITI Types”, 9-32, DITI Index)."
     *                 TL;DR, the Tecan has a global int counter for diti types. FML. Apparently the default is zero.
     *                 IDK if we'll need to change this, so it'll default to zero for now.
     * @param lihaNum which LiHa to use. If only one is installed (typically), this will be zero (the default). There
     *                can be up to two LiHas installed. So the value can be 0 or 1 depending on which you want to use.
     *                Keep in mind that you can't magically levitate fluid from one LiHa to another, so if you aspirate
     *                in one, you'll need to dispense from the same.
     * @param deck
     */
    case class PickUpTips(tipsToUse: List[Boolean],
                          plateN: String,
                          rowNum: Int,
                          deck: TecanDeck,
                          ditiEnum: Int,
                          lihaNum: Int) extends TecanCommand {
      override def mkLine: String = {
        val plateCoords = deck.coordsFromPlateName(plateN)
        val plateType = deck.plateTypeFromName(plateN)
        // TODO: this 1 indexing may not actually be correct if the base tip racks near the trash are index 0. Further wokr is needed.
        s"""PickUp_DITIs2(${tipMask(tipsToUse)},${(plateCoords._2 + 1) * 6},${plateCoords._1},
           |"${wellEncoding(rowNum, 12, 8)}",0,"$plateType",$lihaNum);""".stripMargin.replace("\n", "")
      }
    }

    object PickUpTips{
      def apply(tipsToUse: List[Boolean],
                plateN: String,
                rowNum: Int,
                ditiEnum: Int,
                lihaNum: Int = 0)(implicit deck: TecanDeck): PickUpTips = {
        PickUpTips(tipsToUse, plateN, rowNum, deck, ditiEnum, lihaNum)
      }
    }


    /**
     * Wash out the tips so they can in theory be re-used. In practice, IDK if this is actually useful, but the Tecan
     * supports it, so we should probably consider it.
     *
     * This assumes that that the location of the trash and wash and cleaner locations are fixed. We may want to improve
     * this eventually.
     *
     * For what these parameters mean, look in the manual. It does a better job with this than we can here.
     */
    case class Wash(tipsToUse: List[Boolean],
                    wasteVol: Volume,
                    wasteDelay: Time,
                    cleanerVol: Volume,
                    cleanerDelay: Time,
                    airGap: Volume,
                    airGapSpeed: VolumeFlow,
                    retractSpeed: Velocity,
                    useFastWash: Boolean,
                    lowVol: Boolean,
                    lihaNum: Int) extends TecanCommand {
      override def mkLine: String = {
        // TODO: This assumes that that the location of the trash and wash and cleaner locations are fixed. We may want
        //  to improve this eventually.
        def bToI(bool: Boolean): Int = {
          if(bool) 1 else 0
        }
        s"""Wash(${tipMask(tipsToUse, 12)},2,1,2,2,
           |"${wasteVol.toMicrolitres}",${wasteDelay.toMilliseconds.toInt},
           |"${cleanerVol.toMicrolitres}",${cleanerDelay.toMilliseconds.toInt},
           |${airGap.toMicrolitres.toInt},${(airGapSpeed.toCubicMetersPerSecond*scala.math.pow(1000.0,3)).toInt},
           |${(retractSpeed.toMetersPerSecond*1000).toInt},
           |${bToI(useFastWash)},${bToI(lowVol)},0,$lihaNum);""".stripMargin.replace("\n", "")
      }
    }

    object Wash{
      def apply(tipsToUse: List[Boolean],
                wasteVol: Volume = Microlitres(2),
                wasteDelay: Time = Milliseconds(500),
                cleanerVol: Volume = Microlitres(1),
                cleanerDelay: Time = Milliseconds(500),
                airGap: Volume = Microlitres(10),
                airGapSpeed: VolumeFlow = Microlitres(70)/Seconds(1),
                retractSpeed: Velocity = Millimeters(30)/Seconds(1),
                useFastWash: Boolean = false,
                lowVol: Boolean = false,
                lihaNum: Int = 0): Wash = {
        new Wash(tipsToUse, wasteVol, wasteDelay, cleanerVol, cleanerDelay, airGap, airGapSpeed, retractSpeed, useFastWash, lowVol, lihaNum)
      }
    }

    case class DropTips(tipsToUse: List[Boolean], lihaNum: Int = 0) extends TecanCommand {
      // TODO: For now we're hardcoding where the trash is. This is prolly fine, but we might want to fix this. It'll
      //  just involve actually integrating with Prism offsets in ways we've not so far.
      // DropDITI(255,2,2,10,70,0);
      override def mkLine: String = {
        s"""DropDITI(${tipMask(tipsToUse)},2,2,10,70,$lihaNum);"""
      }
    }
  }

  sealed trait TecanRequest
  case class WriteTecanProtocol(cmds: List[TecanCommand], fileSavePath: Option[String] = None) extends TecanRequest

  sealed trait TecanEvent
  case class ProtocolGenerated(protocol: String, wroteTo: String) extends TecanEvent
  case class GenerationFailure(source: List[TecanCommand], error: String) extends TecanEvent
  case class FileWriteFailure(protocol: String, attemptedPath: String, error: String) extends TecanEvent

  class TecanPlateSerializer extends AvroSerializer[TecanPlate]
  class TecanDeckSerializer extends AvroSerializer[TecanDeck]
  class TecanFluidClassSerializer extends AvroSerializer[TecanFluidClass]

  class DropTipsSerializer extends AvroSerializer[DropTips]
  class PickUpTipsSerializer extends AvroSerializer[PickUpTips]
  class MixSerializer extends AvroSerializer[Mix]
  class WashSerializer extends AvroSerializer[Wash]
  class DispenseSerializer extends AvroSerializer[Dispense]
  class AspirateSerializer extends AvroSerializer[Aspirate]

  class WriteTecanProtocolSerializer extends AvroSerializer[WriteTecanProtocol]

  class ProtocolGeneratedSerializer extends AvroSerializer[ProtocolGenerated]
  class GenerationFailureSerializer extends AvroSerializer[GenerationFailure]
  class FileWriteFailureSerializer extends AvroSerializer[FileWriteFailure]

}
