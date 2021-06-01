package com.radix.shared.persistence.serializations.squants

import squants.motion.{VolumeFlow, VolumeFlowRateUnit}
import squants.space.{CubicMeters, Litres}
import squants.time.Time

object units {

  object LitresPerMinute extends VolumeFlowRateUnit {
    def symbol: String = "L/min"

    protected def conversionFactor: Double =
      Litres.conversionFactor / CubicMeters.conversionFactor / Time.SecondsPerMinute
  }

  implicit class MoreVolumeFlowConversions[A](n: A)(implicit num: Numeric[A]) {
    def litresPerMinute: VolumeFlow = LitresPerMinute(n)
  }

}
