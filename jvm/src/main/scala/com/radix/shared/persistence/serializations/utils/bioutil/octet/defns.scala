package com.radix.shared.persistence.serializations.utils.bioutil.octet

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import shapeless.the
import squants.{Temperature, Time}
import com.radix.shared.persistence.serializations.squants.Serializers._

object defns {

  object Plate {
    sealed trait PlateRows {
      val rowNumber: Int
    }
    case object Row1 extends PlateRows {
      override val rowNumber: Int = 1
    }
    case object Row2 extends PlateRows {
      override val rowNumber: Int = 2
    }
    case object Row3 extends PlateRows {
      override val rowNumber: Int = 3
    }
    case object Row4 extends PlateRows {
      override val rowNumber: Int = 4
    }
    case object Row5 extends PlateRows {
      override val rowNumber: Int = 5
    }
    case object Row6 extends PlateRows {
      override val rowNumber: Int = 6
    }
    case object Row7 extends PlateRows {
      override val rowNumber: Int = 7
    }
    case object Row8 extends PlateRows {
      override val rowNumber: Int = 8
    }
    case object Row9 extends PlateRows {
      override val rowNumber: Int = 9
    }
    case object Row10 extends PlateRows {
      override val rowNumber: Int = 10
    }
    case object Row11 extends PlateRows {
      override val rowNumber: Int = 11
    }
    case object Row12 extends PlateRows {
      override val rowNumber: Int = 12
    }
    case object AllRows extends PlateRows {
      override val rowNumber: Int = 1 // TODO: check
    }
    case class SingleWell(id: String) extends PlateRows {
      override val rowNumber: Int = id.drop(1).toInt

    }


    implicit val plateRowsEncoder: Encoder[PlateRows] = deriveEncoder[PlateRows]
    implicit val plateRowsDecoder: Decoder[PlateRows] = deriveDecoder[PlateRows]


  }

  object LaserHead {
    sealed trait ChannelConfiguration {
      val numberOfChannels: Int
    }
    case object SingleChannel extends ChannelConfiguration {
      override val numberOfChannels: Int = 1
    }
    case class MultiChannel(numberOfChannels: Int) extends ChannelConfiguration
    object MultiChannel {
      def apply(n: Int): MultiChannel = {
        if (n != 8 && n != 96 && n != 384) {
          throw new IllegalArgumentException(s"$n channel sensor heads are not supported")
        }
        new MultiChannel(n)
      }
    }

    implicit val plateRowsEncoder: Encoder[ChannelConfiguration] = deriveEncoder[ChannelConfiguration]
    implicit val plateRowsDecoder: Decoder[ChannelConfiguration] = deriveDecoder[ChannelConfiguration]
  }

  // TODO: add sensor material?
  // TODO: add plate configuration? (map well Id -> protein/sensor type/etc)
  case class OctetConfiguration(channels: LaserHead.ChannelConfiguration)

  sealed trait OctetCommand
  case class Interferometer(rowToSample: Plate.PlateRows,
                            timeToSample: Time,
                            temperature: Temperature,
                            integrationTime: Time)
    extends OctetCommand

  case class OctetProtocol(commands: List[OctetCommand],
                           octetConfiguration: OctetConfiguration)

  implicit def octetConfigurationEncoder: Encoder[OctetConfiguration] = deriveEncoder[OctetConfiguration]
  implicit def octetConfigurationDecoder: Decoder[OctetConfiguration] = deriveDecoder[OctetConfiguration]

  implicit def interferometerEncoder: Encoder[OctetCommand] = deriveEncoder[OctetCommand]
  implicit def interferometerDecoder: Decoder[OctetCommand] = deriveDecoder[OctetCommand]

  implicit def octetProtocolEncoder: Encoder[OctetProtocol] = deriveEncoder[OctetProtocol]
  implicit def octetProtocolDecoder: Decoder[OctetProtocol] = deriveDecoder[OctetProtocol]

}
