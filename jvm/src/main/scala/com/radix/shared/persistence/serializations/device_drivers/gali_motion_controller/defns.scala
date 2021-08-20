package com.radix.shared.persistence.serializations.device_drivers.gali_motion_controller

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef

import com.radix.shared.persistence.serializations.device_drivers.gali_motion_controller.defns.GaliBaseCommandSet._
import com.radix.shared.persistence.serializations.device_drivers.gali_motion_controller.defns.OctetGali._

import squants.space.{Length, Millimeters}
import java.time.LocalDateTime

import scala.concurrent.duration.{Duration, DurationInt}
import akka.util.Timeout
import language.postfixOps

import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.serializations.squants.units._

object defns {
  object GaliBaseCommandSet {
    sealed trait GaliResponse
    sealed trait GaliCmd {
      def cmdString: String
      def parseResponse(s: String): GaliResponse

      def timeout: Duration = 3 seconds
    }

    def safeToInt(s: String): Option[Int] = {
      try {
        Some(s.toInt)
      } catch {
        case e: Exception => None
      }
    }

    def safeToDouble(s: String): Option[Double] = {
      try {
        Some(s.toDouble)
      } catch {
        case e: Exception => None
      }
    }

    //  def safeToMM(n: Double): Option[Millimeters] = {
    //    try {
    //      Some(Millimeters(n))
    //    } catch {
    //      case e: Exception => None
    //    }
    //  }

    case class GaliPosition(x: Length, y: Length, z: Length) extends GaliResponse
    case class GaliNDegreeSpeed(speeds: List[Tuple2[Int, Double]]) extends GaliResponse
    case class GaliNDegreePosition(poses: List[Tuple2[Int, Length]]) extends GaliResponse
    case class GaliPorpConstsResp(poses: List[Tuple2[Int, Double]]) extends GaliResponse

    /** the program from the controller */
    case class GaliFirmwareProgram(program: String) extends GaliResponse
    case class GaliStringResponse(resp: String) extends GaliResponse
    case class GaliCtrlerStatuses(statuses: Seq[GaliCtrlStatus]) extends GaliResponse
    case class GaliNoResp() extends GaliResponse
    case class GaliCtrlDate(date: java.time.LocalDateTime) extends GaliResponse

    sealed trait GaliCtrlStatus
    case class ExecutingAplicationProg() extends GaliCtrlStatus
    case class Contoring() extends GaliCtrlStatus
    case class ExecutingErrorOrLimitSwitchRoutine() extends GaliCtrlStatus
    case class InputInteruptEnabled() extends GaliCtrlStatus
    case class InputInteruptExecuting() extends GaliCtrlStatus
    case class EchoOn() extends GaliCtrlStatus

    /**
     * Stop all motion. Think of this as a near real-time (bounded by the USB and OS latency) software E-Stop.
     */
    case class GaliStop() extends GaliCmd {
      override def cmdString: String = "ST"
      override def parseResponse(s: String): GaliNoResp = GaliNoResp()
    }

    /**
     * End a subroutine. Use it to jump out of infinate loops in the fimware.
     */
    case class GaliEnd() extends GaliCmd {
      override def cmdString: String = "EN"
      override def parseResponse(s: String): GaliNoResp = GaliNoResp()
    }

    case class GaliGetVelocity(numAxies: Int = 3) extends GaliCmd {
      val axisLetters = ('A' to ('A'.toInt + numAxies - 1).toChar).mkString
      override def cmdString: String = s"TV $axisLetters"

      override def parseResponse(s: String): GaliNDegreeSpeed = {
        val speeds: List[Tuple2[Int, Double]] = s
          .split(",")
          .zipWithIndex
          .map(e => (e._2, safeToDouble(e._1)))
          .collect {
            case (index, Some(velDub)) if velDub > 0.0 => (index, velDub)
          }
          .toList
        GaliNDegreeSpeed(speeds)
      }
    }

    /**
     * Parse the gali n degree position string into the map. In typical use, this will be:
     * 1 == X
     * 2 == Y
     * 3 == Z
     * @param s The response string from the controller to parse
     * @return A map of the axis number to the reported position.
     */
    def parseIntoIndexedMap(s: String): Map[Int, Length] = {
      s.split(",")
        .zipWithIndex
        .flatMap(p => {
          val num: Option[Double] = safeToDouble(p._1)
          num match {
            case None        => None
            case Some(value) => Some((p._2, Millimeters(value)))
          }
        })
        .toMap
    }

    /**
     * Gets the position of the controller relative to where it was when it powered on.
     */
    case class GaliAbsolutePosition(numAxies: Int = 3) extends GaliCmd {
      override def cmdString: String = s"PA ?${",?" * (numAxies - 1)}"
      override def parseResponse(s: String): GaliNDegreePosition = {
        val poses = parseIntoIndexedMap(s)
        GaliNDegreePosition(poses.toList)
      }
    }

    /**
     * Get the full firmware in the Gali scripting languague from the firmware. Useful to confirm versioning & similar.
     */
    case class GaliGetEmbededFirmware() extends GaliCmd {
      override def cmdString: String = s"LS"

      // This one takes a LONG time to return, so we'll increase the timeout.
      override val timeout: Duration = 20.seconds

      override def parseResponse(s: String): GaliFirmwareProgram = {
        GaliFirmwareProgram(s)
      }
    }

    /** sically println but for an embeded RS232 device. Useful for confirming comms and maybe confirming syncronization. */
    case class GaliPrintStr(strToPrint: String) extends GaliCmd {
      override def cmdString: String = s"""MG "$strToPrint" """
      override def parseResponse(s: String): GaliStringResponse = {
        GaliStringResponse(s)
      }
    }

    /**
     * I think this might not be useful because it'll typically only return the status code when a command failed. I'm
     * implementing it against future need. Dont' use it without testing it. Adding the one behind it gives us the human
     * readable error code. I didn't want to match on a ton of different error codes, so that's going to be good enough
     * for now.
     */
    case class GaliErrorCode() extends GaliCmd {
      override def cmdString: String = s"""TC1"""
      override def parseResponse(s: String): GaliStringResponse = {
        GaliStringResponse(s)
      }
    }

    case class GaliCtrlTime() extends GaliCmd {
      override def cmdString: String = s"""MG TIME"""
      override def parseResponse(s: String): GaliCtrlDate = {
        val date = LocalDateTime.parse(s.toCharArray)
        GaliCtrlDate(date)
      }
    }

    case class GaliControllerStatus() extends GaliCmd {
      override def cmdString: String = s"""TB"""
      override def parseResponse(s: String): GaliCtrlerStatuses = {
        val respCode = safeToInt(s).get
        val statusCodes = respCode.toBinaryString.toArray.reverse

        val powerToErrorMap: Map[Int, GaliCtrlStatus] = Map(
          0 -> EchoOn(),
          // 1 is not used.
          2 -> InputInteruptExecuting(),
          3 -> InputInteruptEnabled(),
          4 -> ExecutingErrorOrLimitSwitchRoutine(),
          5 -> Contoring(),
          // 6 is not used.
          7 -> ExecutingAplicationProg()
        )

        val statusTypeSeq = statusCodes.zipWithIndex.flatMap(a => {
          val power = a._2
          val statusChar = a._1
          if (statusChar == '1') {
            powerToErrorMap.get(power)
          } else { None }
        })

        GaliCtrlerStatuses(statusTypeSeq)
      }
    }

    //  case class GaliPorpConst(numAxies: Int = 3) extends GaliCmd {
    ////    def cmdString: String = "KP ?,?,?"
    //    def cmdString: String = s"KP ?${",?" * (numAxies - 1)}"
    //
    //    override def parseResponse(resp: String): GaliNDegreePosition = {
    //      // expected response is
    //      val poses = resp.split(",").zipWithIndex.flatMap(p => {
    //        val num: Option[Double] = safeToDouble(p._1)
    //        num match {
    //          case None => None
    //          case Some(value) => Some((p._2, Millimeters(value)))
    //        }
    //      }).toMap
    //      GaliNDegreePosition(poses)
    //    }
    //  }

    case class GaliPorpConsts(numAxies: Int = 3) extends GaliCmd {
      //    def cmdString: String = "KP ?,?,?"
      def cmdString: String = s"KP ?${",?" * (numAxies - 1)}"

      override def parseResponse(resp: String): GaliPorpConstsResp = {
        // expected response is
        val poses = resp
          .split(",")
          .zipWithIndex
          .flatMap(p => {
            val num: Option[Double] = safeToDouble(p._1)
            num match {
              case None        => None
              case Some(value) => Some((p._2, value))
            }
          })
          .toMap
        GaliPorpConstsResp(poses.toList)
      }
    }

    def isStillMoving: Boolean = {
      true
    }
  }

  object OctetGali {

    sealed trait OctetGaliEvent
    case class CommandStarted(command: OctetMotionCtrlrCommand) extends OctetGaliEvent
    case class CommandSuccess() extends OctetGaliEvent
    case class CommandFailure() extends OctetGaliEvent

    sealed trait OctetMotionCtrlrRequest

    case class CommandFinished(success: Boolean) extends OctetMotionCtrlrRequest
    sealed trait OctetMotionCtrlrCommand extends OctetMotionCtrlrRequest with GaliCmd {
      val replyTo: Option[ActorRef[OctetMotionResp]]
      override def parseResponse(s: String): OctetMotionResp
    }
    sealed trait OctetMotionResp extends GaliResponse
    case class OctetNoResp() extends OctetMotionResp
    sealed trait OctetPassOrFailResp extends OctetMotionResp
    case class OctetCmdSuccess() extends OctetPassOrFailResp
    case class OctetCmdFail() extends OctetPassOrFailResp

    /**
     * Home X, Y, and Z motor axies.
     */
    case class HomeAll(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #HOMEALL"
      override def parseResponse(s: String): OctetPassOrFailResp = {
        if (s.contains("HOMING COMPLETE!")) {
          OctetCmdSuccess()
        } else {
          OctetCmdFail()
        }
      }
    }

    /**
     * Home just the X axis. This doesn't return a response code.
     * It seems the Octet would have you run HomeAll, but I think this is useful to do homing per axis, especially for
     * testing, so Halie the Honeybadger decided to support it. Don't put a GOTO in your embeded code if you don't want
     * me to GOTO it.
     */
    case class HomeX(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #HOME_X"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    /**
     * Home just the Y axis. This doesn't return a response code.
     * It seems the Octet would have you run HomeAll, but I think this is useful to do homing per axis, especially for
     * testing, so Halie the Honeybadger decided to support it. Don't put a GOTO in your embeded code if you don't want
     * me to GOTO it.
     */
    case class HomeY(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #HOME_Y"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    /**
     * Home just the Z axis. This doesn't return a response code.
     * It seems the Octet would have you run HomeAll, but I think this is useful to do homing per axis, especially for
     * testing, so Halie the Honeybadger decided to support it. Don't put a GOTO in your embeded code if you don't want
     * me to GOTO it.
     */
    case class HomeZ(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #HOME_Z"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    case class Init(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #INIT"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    case class ComputeHardCodeConsts(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #HARDCOD"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    case class TurnOnSpectChannel(channelNum: Int, replyTo: Option[ActorRef[OctetMotionResp]])
        extends OctetMotionCtrlrCommand {
      override def cmdString: String = s"SB$channelNum"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    case class TurnOffSpectChannel(channelNum: Int, replyTo: Option[ActorRef[OctetMotionResp]])
        extends OctetMotionCtrlrCommand {
      override def cmdString: String = s"CB$channelNum"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    /**
     * The program that runs when an Octet powers on. This should run automatically when it powers on without us prompting
     * it, but it's useful for doing a soft-ish reset without power cycling the spectrophotometer. It homes all the axies
     * and does some other math to make the Octet's firmware code work correctly.
     */
    case class AutoStartup(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      // we need the ST command to exit the jerks' infinate loop and be able to run new programs.
      override def cmdString: String =
        "XQ #AUTO\rST" //TODO test that this works correctly. We may not actually want this.
      override def parseResponse(s: String): OctetPassOrFailResp = {
        if (s.contains("START THREAD 0 LOOP")) {
          OctetCmdSuccess()
        } else {
          OctetCmdFail()
        }
      }
    }

    /**
     * Pick up the sensor tips from the tip rack.
     * @param sensorRow The row of sensors we want to use.
     */
    case class PickUpSensor(sensorRow: Int, replyTo: Option[ActorRef[OctetMotionResp]])
        extends OctetMotionCtrlrCommand {
      // vSensrOn is a bool set in code without sensor feedback that the tips were picked up. It's not a perfect metric,
      // but since it's all the validation we have available, we'll use it.
      override def cmdString: String = s"vCmdSens = $sensorRow \rXQ #PICK \rMG vSensrOn"
      override def parseResponse(s: String): OctetPassOrFailResp = {
        if (s.contains("1")) {
          /* Example response. And yes, that is a floating point boolean.
          :vCmdSens = 1
          :XQ #PICK
          :MG vSensrOn
           1.0000
           */
          OctetCmdSuccess()
        } else {
          OctetCmdFail()
        }
      }
    }

    case class EjectSensor(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      // vSensrOn is a bool set in code without sensor feedback that the tips were picked up. It's not a perfect metric,
      // but since it's all the validation we have available, we'll use it.
      override def cmdString: String = s"XQ #EJECT \rMG vSensrOn"
      override def parseResponse(s: String): OctetPassOrFailResp = {
        // The response is a bool represented with floating point. So true will have trailing zeros. Se we check instead
        // if it has a zero followed by a period AKA is the first digit zero.
        if (s.contains("0.")) {
          OctetCmdSuccess()
        } else {
          OctetCmdFail()
        }
      }
    }

    case class Sample(sensorRow: Int, replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      // vSensrOn is a bool set in code without sensor feedback that the tips were picked up. It's not a perfect metric,
      // but since it's all the validation we have available, we'll use it.
      override def cmdString: String = s"vCmdSamp = $sensorRow \rXQ #SAMPLE"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

    /**
     * Clear interlock from (I think) the door opening during a run. This may automatically trigger from the firmware when
     * the door(?) is closed.
     * This will error/not do anything if there's not an interlock. I don't know if we'll want to use it, but since the
     * code supports it, I figure we should, too.
     */
    case class ResetInterlock(replyTo: Option[ActorRef[OctetMotionResp]]) extends OctetMotionCtrlrCommand {
      override def cmdString: String = "XQ #RSTITLK"
      override def parseResponse(s: String): OctetNoResp = OctetNoResp()
    }

  }

  class GaliPorpConstsRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliPorpConsts]
  class GaliControllerStatusSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliControllerStatus]
  class GaliCtrlTimeSerializer extends AvroSerializer[GaliCtrlTime]
  class GaliErrorCodeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliErrorCode]
  class GaliPrintStrSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliPrintStr]
  class GaliGetEmbededFirmwareSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[GaliGetEmbededFirmware]
  class GaliAbsolutePositionSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliAbsolutePosition]
  class GaliStopSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliStop]
  class EchoOnSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[EchoOn]
  class InputInteruptExecutingSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[InputInteruptExecuting]
  class InputInteruptEnabledSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[InputInteruptEnabled]
  class ExecutingErrorOrLimitSwitchRoutineSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[ExecutingErrorOrLimitSwitchRoutine]
  class ContoringSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Contoring]
  class ExecutingAplicationProgSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[ExecutingAplicationProg]
  class GaliCtrlStatusSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliCtrlStatus]
  class GaliCtrlDateSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliCtrlDate]
  class GaliNoRespSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliNoResp]
  class GaliCtrlerStatusesSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliCtrlerStatuses]
  class GaliStringResponseSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliStringResponse]
  class GaliFirmwareProgramSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliFirmwareProgram]
  class GaliCmdSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliCmd]

  class GaliPorpConstsRespSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliPorpConstsResp]
  class GaliNDegreePositionSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliNDegreePosition]
  class GaliPositionSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliPosition]
  // we serialize the sub types, so we shouldn't need this:
//  class GaliResponseSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[GaliResponse]

//  class OctetGaliEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetGaliEvent]
  class CommandSuccessSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandSuccess]
  class CommandFailureSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandFailure]
  class CommandStartedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[CommandStarted]
  class OctetMotionCtrlrRequestSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OctetMotionCtrlrRequest]
  class OctetMotionCtrlrCommandSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OctetMotionCtrlrCommand]
  class OctetMotionRespSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetMotionResp]
  class OctetNoRespSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetNoResp]
  class OctetPassOrFailRespSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetPassOrFailResp]
  class OctetCmdSuccessSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetCmdSuccess]
  class OctetCmdFailSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OctetCmdFail]
  class HomeAllSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HomeAll]
  class HomeXSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HomeX]
  class HomeYSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HomeY]
  class HomeZSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[HomeZ]
  class AutoStartupSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[AutoStartup]
  class PickUpSensorSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[PickUpSensor]
  class EjectSensorSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[EjectSensor]
  class SampleSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[Sample]
  class ResetInterlockSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[ResetInterlock]

}
