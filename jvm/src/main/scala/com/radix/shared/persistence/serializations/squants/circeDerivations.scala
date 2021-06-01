package com.radix.shared.persistence.serializations.squants

import io.circe._
import squants.space.{Length, Volume}
import squants.thermal.Temperature
import squants.{Temperature, Volume} //this is necessary

object Serializers {
  implicit val tempEncoder: Encoder[Temperature] = Encoder.encodeString.contramap { _.toString }
  implicit val tempDecoder: Decoder[Temperature] = Decoder.decodeString.emap { str =>
    Temperature(str).toOption.toRight("temperature")
  }

  implicit val volEncoder: Encoder[Volume] = Encoder.encodeString.contramap { _.toString }
  implicit val volDecoder: Decoder[Volume] = Decoder.decodeString.emap { str =>
    Volume(str).toOption.toRight("volume")
  }

  implicit val lenEncoder: Encoder[Length] = Encoder.encodeString.contramap { _.toString }
  implicit val lenDecoder: Decoder[Length] = Decoder.decodeString.emap { str =>
    Length(str).toOption.toRight("length")
  }

}
