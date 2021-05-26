package com.radix.shared.persistence.serializations.device_drivers.minifors2

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._
import com.radix.shared.persistence.serializations.squants.units.LitresPerMinute
import squants.mass.Grams
import squants.space.{Millilitres, Volume}
import squants.thermal.Celsius
import squants.time.{Frequency, RevolutionsPerMinute}
import squants.{Mass, Temperature, VolumeFlow}

object defns {

  /**
   * Something that can happen in the Minifors2 driver to change its state.
   */
  sealed trait Minifors2Event

  /**
   * An event in which a request was sent to the bioreactor and a response was received.
   */
  case class RequestEvent(request: Minifors2Request, response: Minifors2Response) extends Minifors2Event

  class RequestEventSerializer(implicit eas: ExtendedActorSystem) extends
    AvroSerializer[RequestEvent]

  /**
   * An event in which a request was sent to the bioreactor but the driver was in a non-connected state.
   */
  case class NotConnectedRequestEvent(request: Minifors2Request) extends Minifors2Event

  class NotConnectedSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[NotConnectedRequestEvent]

  /**
   * A request sent to the bioreactor.
   */
  sealed trait Minifors2Request {
    /**
     * The actor to send the bioreactor's response to.
     */
    val replyTo: Option[ActorRef[Minifors2Response]]
  }


  /**
   * A response received from the bioreactor.
   */
  sealed trait Minifors2Response

  /**
   * The request could not be handled because the machine is offline.
   */
  case object MachineOffline extends Minifors2Response

  /**
   * A parameter that the bioreactor can measure and control
   *
   * @param id The string the bioreactor uses to identify the parameter
   */
  sealed abstract class ParameterType(val id: String)

  /**
   * Parameters that describe pumps
   */
  sealed trait PumpParameterType extends ParameterType

  object ParameterTypes {

    case object Pump1 extends ParameterType("Pump1") with PumpParameterType

    case object Pump2 extends ParameterType("Pump2") with PumpParameterType

    case object Pump3 extends ParameterType("Pump3") with PumpParameterType

    case object Pump4 extends ParameterType("Pump4") with PumpParameterType

    case object Foam extends ParameterType("Foam")

    case object AirFlow extends ParameterType("AirFlow")

    // While this parameter is called 'GMFlow' inside of the OPC database, it's referred to as TotalFlow
    // on the machine and in the user manual.
    case object TotalFlow extends ParameterType("GMFlow")

    case object Gas2Flow extends ParameterType("Gas2Flow")

    case object GasMix extends ParameterType("GasMix")

    case object StirrerSpeed extends ParameterType("StirrerSpeed")

    case object Temperature extends ParameterType("Temperature")

    case object PH extends ParameterType("pH")

    case object PO2 extends ParameterType("pO2")

    case object ExitCO2 extends ParameterType("ExitCO2")

    case object ExitHumidity extends ParameterType("ExitHumidity")

    case object ExitO2 extends ParameterType("ExitO2")

    case object OpticalDensity extends ParameterType("OD")

    case object Balance extends ParameterType("Balance1")

    case object AnalogIO1 extends ParameterType("AnalogIO1")

    case object AnalogIO2 extends ParameterType("AnalogIO2")

  }


  /**
   * A field containing information about a certain parameter
   *
   * @param id String used by bioreactor to identify field
   */
  sealed abstract class FieldType(val id: String)

  /**
   * Fields that are only relevant to pumps
   */
  sealed trait PumpParameterFieldType extends FieldType

  /**
   * Field that can be read from the server
   */
  sealed trait ReadableFieldType extends FieldType

  /**
   * Field that can be written to the server
   */
  sealed trait WriteableFieldType extends FieldType

  object FieldTypes {

    case object CascadeSourceParameter extends FieldType("CascadeSourceParameter") with ReadableFieldType with WriteableFieldType

    case object MinValue extends FieldType("MinValue") with ReadableFieldType with WriteableFieldType

    case object MaxValue extends FieldType("MaxValue") with ReadableFieldType with WriteableFieldType

    case object Name extends FieldType("Name") with ReadableFieldType with WriteableFieldType

    case object SetPoint extends FieldType("SetPoint") with ReadableFieldType with WriteableFieldType

    case object MeasurementUnit extends FieldType("Unit") with ReadableFieldType with WriteableFieldType

    case object Exists extends FieldType("Exist") with ReadableFieldType with WriteableFieldType

    case object Value extends FieldType("Value") with ReadableFieldType

    case object PumpFactor extends FieldType("PumpFactor") with PumpParameterFieldType with ReadableFieldType with WriteableFieldType

  }


  /**
   * Converts a float to a squants value of type T.
   */
  sealed trait SquantConverter[T] {
    def conv(in: Float): T
  }

  implicit object VFlowSquantConverter$ extends SquantConverter[VolumeFlow] {
    override def conv(in: Float): VolumeFlow = LitresPerMinute(in)
  }

  implicit object UnitlessSquantConverter$ extends SquantConverter[Float] {
    override def conv(in: Float): Float = in
  }

  implicit object FreqSquantConverter$ extends SquantConverter[Frequency] {
    override def conv(in: Float): Frequency = RevolutionsPerMinute(in)
  }

  implicit object TempSquantConverter$ extends SquantConverter[Temperature] {
    override def conv(in: Float): Temperature = Celsius(in)
  }

  implicit object MassSquantConverter$ extends SquantConverter[Mass] {
    override def conv(in: Float): Mass = Grams(in)
  }

  implicit object VolumeSquantConverter extends SquantConverter[Volume] {
    override def conv(in: Float): Volume = Millilitres(in)
  }


  /**
   * Contains information read from the bioreactor about a parameter.
   *
   * @param setpointConv Converter to convert setpoint from a float to a squant unit
   * @param valConv      Converter to convert value from a float to a squant unit
   * @tparam S The squants type of the parameter's setpoint. Use Float if the setpoint is unitless.
   * @tparam V The squants type of the parameter's actual value. Use Float if the value is unitless.
   */
  sealed abstract class ParameterInfo[S, V](implicit setpointConv: SquantConverter[S], valConv: SquantConverter[V]) {
    val cascadeSourceParameter: String
    val rawMinValue: Float
    val rawMaxValue: Float
    val name: String
    val rawSetPoint: Float
    val rawValue: Float
    val unit: String
    val value: V = valConv.conv(rawValue)
    val minValue: S = setpointConv.conv(rawMinValue)
    val maxValue: S = setpointConv.conv(rawMaxValue)
    val setPoint: S = setpointConv.conv(rawSetPoint)
  }

  /**
   * ParameterInfo where the setpoint and value have the same squants type.
   *
   * @tparam U Squants type of setpoint and value of parameter, or Float if they're unitless.
   */
  sealed abstract class UniformParameterInfo[U](implicit conv: SquantConverter[U]) extends ParameterInfo[U, U]()(conv, conv)


  case class PumpInfo(pumpName: PumpParameterType, cascadeSourceParameter: String,
                      rawMinValue: Float, rawMaxValue: Float,
                      name: String, rawSetPoint: Float, unit: String, rawValue: Float, pumpFactor: Float) extends
    ParameterInfo[Float, Volume] {
    override val value: Volume = Millilitres(rawValue * pumpFactor)
  }


  case class FoamInfo(cascadeSourceParameter: String,
                      rawMinValue: Float, rawMaxValue: Float,
                      name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class AirFlowInfo(cascadeSourceParameter: String,
                         rawMinValue: Float, rawMaxValue: Float,
                         name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[VolumeFlow]

  case class TotalFlowInfo(cascadeSourceParameter: String,
                           rawMinValue: Float, rawMaxValue: Float,
                           name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[VolumeFlow]

  case class Gas2FlowInfo(cascadeSourceParameter: String,
                          rawMinValue: Float, rawMaxValue: Float,
                          name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[VolumeFlow]

  case class GasMixInfo(cascadeSourceParameter: String,
                        rawMinValue: Float, rawMaxValue: Float,
                        name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class StirrerSpeedInfo(cascadeSourceParameter: String,
                              rawMinValue: Float, rawMaxValue: Float,
                              name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Frequency]

  case class TemperatureInfo(cascadeSourceParameter: String,
                             rawMinValue: Float, rawMaxValue: Float,
                             name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Temperature]

  case class PHInfo(cascadeSourceParameter: String,
                    rawMinValue: Float, rawMaxValue: Float,
                    name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class PO2Info(cascadeSourceParameter: String,
                     rawMinValue: Float, rawMaxValue: Float,
                     name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class ExitCO2Info(cascadeSourceParameter: String,
                         rawMinValue: Float, rawMaxValue: Float,
                         name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class ExitHumidityInfo(cascadeSourceParameter: String,
                              rawMinValue: Float, rawMaxValue: Float,
                              name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class ExitO2Info(cascadeSourceParameter: String,
                        rawMinValue: Float, rawMaxValue: Float,
                        name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class OpticalDensityInfo(cascadeSourceParameter: String,
                                rawMinValue: Float, rawMaxValue: Float,
                                name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class BalanceInfo(cascadeSourceParameter: String,
                         rawMinValue: Float, rawMaxValue: Float,
                         name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Mass]

  case class AnalogIO1Info(cascadeSourceParameter: String,
                           rawMinValue: Float, rawMaxValue: Float,
                           name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]

  case class AnalogIO2Info(cascadeSourceParameter: String,
                           rawMinValue: Float, rawMaxValue: Float,
                           name: String, rawSetPoint: Float, unit: String, rawValue: Float) extends UniformParameterInfo[Float]


  case class DeviceMetadata(deviceName: String, processState: String)


  /**
   * Contains all data about the bioreactor's parameters at a certain point in time. The bioreactor may not support
   * all of these parameters based on its configuration; parameters that are not being measured will be set to None.
   */
  case class Summary(deviceMetadata: DeviceMetadata, foam: Option[FoamInfo], airFlow: Option[AirFlowInfo],
                     totalFlow: Option[TotalFlowInfo], gas2Flow: Option[Gas2FlowInfo], gasMix: Option[GasMixInfo],
                     pump1: Option[PumpInfo], pump2: Option[PumpInfo], pump3: Option[PumpInfo],
                     pump4: Option[PumpInfo], stirrerSpeed: Option[StirrerSpeedInfo], temperature: Option[TemperatureInfo],
                     pH: Option[PHInfo], pO2: Option[PO2Info],
                     exitCO2: Option[ExitCO2Info], exitHumidity: Option[ExitHumidityInfo],
                     exitO2: Option[ExitO2Info], opticalDensity: Option[OpticalDensityInfo],
                     balance: Option[BalanceInfo], analogIO1: Option[AnalogIO1Info],
                     analogIO2: Option[AnalogIO2Info]) extends Minifors2Response {

    /**
     * Gets the ParameterInfo of the given ParameterType.
     */
    def getParameterByType(parameter: ParameterType): Option[ParameterInfo[_, _]] =
      parameter match {
        case ParameterTypes.Pump1 => pump1
        case ParameterTypes.Pump2 => pump2
        case ParameterTypes.Pump3 => pump3
        case ParameterTypes.Pump4 => pump4
        case ParameterTypes.Foam => foam
        case ParameterTypes.AirFlow => airFlow
        case ParameterTypes.TotalFlow => totalFlow
        case ParameterTypes.Gas2Flow => gas2Flow
        case ParameterTypes.GasMix => gasMix
        case ParameterTypes.StirrerSpeed => stirrerSpeed
        case ParameterTypes.Temperature => temperature
        case ParameterTypes.PH => pH
        case ParameterTypes.PO2 => pO2
        case ParameterTypes.ExitCO2 => exitCO2
        case ParameterTypes.ExitHumidity => exitHumidity
        case ParameterTypes.ExitO2 => exitO2
        case ParameterTypes.OpticalDensity => opticalDensity
        case ParameterTypes.Balance => balance
        case ParameterTypes.AnalogIO1 => analogIO1
        case ParameterTypes.AnalogIO2 => analogIO2
      }
  }

  class SummarySerializer extends AvroSerializer[Summary]


  /**
   * Requests to get all data on the bioreactor's parameters.
   */
  case class SummaryRequest(replyTo: Option[ActorRef[Minifors2Response]])
    extends Minifors2Request

  class SummaryRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SummaryRequest]

  /**
   * A bioreactor response that it specific to write requests.
   */
  sealed trait WriteResponse extends Minifors2Response

  /**
   * Collection of write results, one for each edited field of a parameter.
   */
  case class ParameterWriteResponses(responses: List[ParameterWriteResult]) extends WriteResponse

  class ParameterWriteResponseSerializer extends AvroSerializer[ParameterWriteResponses]

  /**
   * Contains information about the result of trying to write to the field of a parameter.
   */
  sealed trait ParameterWriteResult {
    val parameterType: ParameterType
    val fieldType: FieldType
  }

  /**
   * A result that can be obtained by trying to write to any field on the machine.
   */
  sealed trait GeneralParameterWriteResult extends ParameterWriteResult

  /**
   * A result that can only be obtained from trying to write a parameter's setpoint.
   */
  sealed trait SetpointWriteResult extends ParameterWriteResult

  /**
   * A value has successfully been written to the given field of the given parameter
   */
  case class SuccessfulWrite(parameterType: ParameterType, fieldType: FieldType) extends GeneralParameterWriteResult with
    SetpointWriteResult

  /**
   * A value could not be written to the given field of the given parameter for reasons relating to the OPC server.
   * The error code provided is an OPC Status Code
   */
  case class WriteRejected(parameterType: ParameterType, fieldType: FieldType, code: Long, message: String) extends GeneralParameterWriteResult with
    SetpointWriteResult

  /** A setpoint cannot be written to a parameter that is controlled by cascade. */
  case class CannotWriteToCascade(parameterType: ParameterType) extends SetpointWriteResult {
    val fieldType: FieldType = FieldTypes.SetPoint
  }

  /** The attempted setpoint write is outside the min/max values set by the bioreactor. */
  case class SetpointOutOfBounds(parameterType: ParameterType, minValue: Float, maxValue: Float) extends SetpointWriteResult {
    val fieldType: FieldType = FieldTypes.SetPoint
  }

  /** The write cannot be completed because the parameter currently does not exist on the machine. */
  case class ParameterDisabled(parameterType: ParameterType, fieldType: FieldType) extends GeneralParameterWriteResult with
    SetpointWriteResult

  /**
   * Request to write data to a node on the bioreactor.
   */
  sealed trait WriteRequest extends Minifors2Request

  sealed trait ParameterWriteRequest extends WriteRequest {
    val parameterType: ParameterType
  }

  sealed trait SetPointWriteRequest extends ParameterWriteRequest {
    val setPoint: Float
  }

  case class WriteTemperature(replyTo: Option[ActorRef[Minifors2Response]], temperatureSetPoint: Temperature)
    extends SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.Temperature
    val setPoint: Float = temperatureSetPoint.to(Celsius).toFloat
  }

  class WriteTemperatureSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WriteTemperature]

  case class WriteTotalFlow(replyTo: Option[ActorRef[Minifors2Response]], flowSetPoint: VolumeFlow)
    extends SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.TotalFlow
    val setPoint: Float = flowSetPoint.to(LitresPerMinute).toFloat
  }

  class WriteTotalFlowSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WriteTotalFlow]

  case class WriteGasMix(replyTo: Option[ActorRef[Minifors2Response]], percentO2: Float) extends
    SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.GasMix
    val setPoint: Float = percentO2
  }

  class WriteGasMixSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WriteGasMix]

  case class WritePump(replyTo: Option[ActorRef[Minifors2Response]], pump: PumpParameterType, percentDeliveryRate: Float)
    extends SetPointWriteRequest {
    val parameterType: ParameterType = pump
    val setPoint: Float = percentDeliveryRate
  }

  class WritePumpSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WritePump]

  case class WriteStirrerSpeed(replyTo: Option[ActorRef[Minifors2Response]], frequency: Frequency)
    extends SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.StirrerSpeed
    val setPoint: Float = frequency.toRevolutionsPerMinute.toFloat
  }

  class WriteStirrerSpeedSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WriteStirrerSpeed]

  case class WritePH(replyTo: Option[ActorRef[Minifors2Response]], pH: Float) extends SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.PH
    val setPoint: Float = pH
  }

  class WritePHSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WritePH]

  case class WritePO2(replyTo: Option[ActorRef[Minifors2Response]], percentO2: Float) extends SetPointWriteRequest {
    val parameterType: ParameterType = ParameterTypes.PO2
    val setPoint: Float = percentO2
  }

  class WritePO2Serializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[WritePO2]

}
