package com.radix.shared.persistence.serializations.device_drivers.omnidriver

import akka.actor.ExtendedActorSystem
import com.radix.shared.persistence.ActorRefSerializer._
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.serializations.squants.schemas._
import akka.actor.typed.ActorRef
import squants.Time
import squants.space.Length

object defns {
  sealed trait OmnidriverEvent

  class OmnidriverEventSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OmnidriverEvent]

  case class OmnidriverRequestResponseEvent(request: OmnidriverRequest, response: OmnidriverResponse)
      extends OmnidriverEvent

  class OmnidriverRequestResponseEventSerializer(implicit eas: ExtendedActorSystem)
      extends AvroSerializer[OmnidriverRequestResponseEvent]

  sealed trait OmnidriverRequest {
    val replyTo: Option[ActorRef[CommonResponse]]
  }

  class OmnidriverRequestSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[OmnidriverRequest]

  sealed trait OmnidriverResponse

  class OmnidriverResponseSerializer extends AvroSerializer[OmnidriverResponse]

  case class GetMetadata(replyTo: Option[ActorRef[MetadataResponse]]) extends OmnidriverRequest

  class GetMetadataSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[GetMetadata]

  sealed trait MetadataResponse extends OmnidriverResponse

  class MetadataResponseSerializer extends AvroSerializer[MetadataResponse]

  case class Metadata(serialNumber: String, model: String) extends MetadataResponse

  class MetadataSerializer extends AvroSerializer[Metadata]

  case class GetAcquisitionParameters(replyTo: Option[ActorRef[AcquisitionParameterResponse]]) extends OmnidriverRequest

  class GetAcquisitionParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[GetAcquisitionParameters]

  case class SetAcquisitionParameters(
    replyTo: Option[ActorRef[AcquisitionParameterResponse]],
    lampOn: Option[Boolean]
  ) extends OmnidriverRequest

  class SetAcquisitionParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[SetAcquisitionParameters]

  sealed trait AcquisitionParameterResponse extends OmnidriverResponse

  class AcquisitionParameterResponseSerializer extends AvroSerializer[AcquisitionParameterResponse]

  case class AcquisitionParameters(
    lampOn: Boolean,
    integrationTime: Time
  ) extends AcquisitionParameterResponse {}

  class AcquisitionParametersSerializer extends AvroSerializer[AcquisitionParameters]

  case class SetIntegrationTime(replyTo: Option[ActorRef[SetIntegrationTimeResponse]], time: Time)
      extends OmnidriverRequest

  class SetIntegrationTimeSerializer(implicit eas: ExtendedActorSystem) extends AvroSerializer[SetIntegrationTime]

  sealed trait SetIntegrationTimeResponse extends OmnidriverResponse

  class SetIntegrationTimeResponseSerializer extends AvroSerializer[SetIntegrationTimeResponse]

  case class IntegrationTimeOutOfBounds(min: Time, max: Time, attemptedTime: Time) extends SetIntegrationTimeResponse

  class IntegrationTimeOutOfBoundsSerializer extends AvroSerializer[IntegrationTimeOutOfBounds]

  case class IntegrationTimeSet(time: Time) extends SetIntegrationTimeResponse

  class IntegrationTimeSetSerializer extends AvroSerializer[IntegrationTimeSet]

  case class GetOperatingParameters(replyTo: Option[ActorRef[OperatingParametersResponse]]) extends OmnidriverRequest

  class GetOperatingParametersSerializer(implicit extendedActorSystem: ExtendedActorSystem)
      extends AvroSerializer[GetOperatingParameters]

  sealed trait OperatingParametersResponse extends OmnidriverResponse

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

  case class GetSpectrum(replyTo: Option[ActorRef[SpectrumResponse]]) extends OmnidriverRequest

  class GetSpectrumSerializer(implicit extendedActorSystem: ExtendedActorSystem) extends AvroSerializer[GetSpectrum]

  sealed trait SpectrumResponse extends OmnidriverResponse

  class SpectrumResponseSerializer extends AvroSerializer[SpectrumResponse]

  case class Spectrum(spectrum: Array[Double], saturated: Boolean, wavelengths: Array[Length]) extends SpectrumResponse

  class SpectrumSerializer extends AvroSerializer[Spectrum]

  sealed trait CommonResponse
      extends OmnidriverResponse
      with MetadataResponse
      with AcquisitionParameterResponse
      with SpectrumResponse
      with OperatingParametersResponse
      with SetIntegrationTimeResponse

  class CommonResponseSerializer extends AvroSerializer[CommonResponse]

  case object MachineOffline extends CommonResponse
}
