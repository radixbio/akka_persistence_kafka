package com.radix.shared.persistence.serializations.device_drivers.oceanoptics

import java.time.Instant

import akka.actor.ExtendedActorSystem
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._
import akka.actor.typed.ActorRef
import squants.Time
import squants.space.Length

object defns {

  sealed trait OceanOpticsEvent

  class OceanOpticsEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OceanOpticsEvent]

  case class OceanOpticsRequestResponseEvent(request: OceanOpticsRequest, response: OceanOpticsResponse) extends OceanOpticsEvent

  class OceanOpticsRequestResponseEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OceanOpticsRequestResponseEvent]

  // TODO: add stability test switching
  sealed trait OceanOpticsRequest {
    val replyTo: Option[ActorRef[CommonResponse]]
  }

  class OceanOpticsRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OceanOpticsRequest]

  sealed trait OceanOpticsResponse

  class OceanOpticsResponseSerializer extends AvroSerializer[OceanOpticsResponse]

  case class GetMetadata(replyTo: Option[ActorRef[MetadataResponse]]) extends OceanOpticsRequest

  class GetMetadataSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[GetMetadata]

  sealed trait MetadataResponse extends OceanOpticsResponse

  class MetadataResponseSerializer extends AvroSerializer[MetadataResponse]

  case class Metadata(serialNumber: String, model: String) extends MetadataResponse

  class MetadataSerializer extends AvroSerializer[Metadata]

  case class GetAcquisitionParameters(replyTo: Option[ActorRef[AcquisitionParameterResponse]]) extends OceanOpticsRequest

  class GetAcquisitionParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[GetAcquisitionParameters]

  case class SetAcquisitionParameters(
    replyTo: Option[ActorRef[AcquisitionParameterResponse]],
    lampOn: Option[Boolean],
  ) extends OceanOpticsRequest

  class SetAcquisitionParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[SetAcquisitionParameters]

  sealed trait AcquisitionParameterResponse extends OceanOpticsResponse

  class AcquisitionParameterResponseSerializer extends AvroSerializer[AcquisitionParameterResponse]

  case class AcquisitionParameters(
    lampOn: Boolean,
    integrationTime: Time
  ) extends AcquisitionParameterResponse {}

  class AcquisitionParametersSerializer extends AvroSerializer[AcquisitionParameters]

  case class SetIntegrationTime(replyTo: Option[ActorRef[SetIntegrationTimeResponse]],
                                time: Time) extends OceanOpticsRequest

  class SetIntegrationTimeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetIntegrationTime]

  sealed trait SetIntegrationTimeResponse extends OceanOpticsResponse

  class SetIntegrationTimeResponseSerializer extends AvroSerializer[SetIntegrationTimeResponse]

  case class IntegrationTimeOutOfBounds(min: Time, max: Time, attemptedTime: Time) extends SetIntegrationTimeResponse

  class IntegrationTimeOutOfBoundsSerializer extends AvroSerializer[IntegrationTimeOutOfBounds]

  case class IntegrationTimeSet(time: Time) extends SetIntegrationTimeResponse

  class IntegrationTimeSetSerializer extends AvroSerializer[IntegrationTimeSet]

  case class GetOperatingParameters(replyTo: Option[ActorRef[OperatingParametersResponse]]) extends OceanOpticsRequest

  class GetOperatingParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[GetOperatingParameters]

  sealed trait OperatingParametersResponse extends OceanOpticsResponse

  class OperatingParametersResponseSerializer extends AvroSerializer[OperatingParametersResponse]

  case class OperatingParameters(
    minimumIntegrationTime: Time,
    maximumIntegrationTime: Time,
    integrationStepIncrement: Time,
    maximumIntensity: Int,
    numberOfPixels: Int,
    numberOfDarkPixels: Int,
    channels: Int
  ) extends OperatingParametersResponse

  class OperatingParametersSerializer extends AvroSerializer[OperatingParameters]

  case class SetStabilityScan(needsStabilityScan: Boolean, replyTo: Option[ActorRef[SetStabilityScanResponse]])
    extends OceanOpticsRequest

  class SetStabilityScanSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetStabilityScan]

  sealed trait SetStabilityScanResponse extends OceanOpticsResponse

  class SetStabilityScanResponseSerializer extends AvroSerializer[SetStabilityScanResponse]

  case object StabilitySetResponse extends SetStabilityScanResponse

  case class GetSpectrum(replyTo: Option[ActorRef[SpectrumResponse]]) extends OceanOpticsRequest

  class GetSpectrumSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[GetSpectrum]

  sealed trait SpectrumResponse extends OceanOpticsResponse

  class SpectrumResponseSerializer extends AvroSerializer[SpectrumResponse]

  case class Spectrum(spectrum: Array[Double], saturated: Boolean, wavelengths: Array[Length], timestamp: Instant) extends SpectrumResponse

  class SpectrumSerializer extends AvroSerializer[Spectrum]

  sealed trait CommonResponse
      extends OceanOpticsResponse
      with MetadataResponse
      with AcquisitionParameterResponse
      with SpectrumResponse
      with OperatingParametersResponse
      with SetIntegrationTimeResponse
      with SetStabilityScanResponse

  class CommonResponseSerializer extends AvroSerializer[CommonResponse]

  case object MachineOffline extends CommonResponse
}
