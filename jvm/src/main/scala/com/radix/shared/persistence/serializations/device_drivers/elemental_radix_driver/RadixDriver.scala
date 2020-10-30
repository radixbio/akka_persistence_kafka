package com.radix.shared.persistence.serializations.device_drivers.elemental_radix_driver

import com.typesafe.config.ConfigFactory
import io.circe.Json

object ElementalRadixDriverTypes {
  trait ElemetalDriverable {
    private val config = ConfigFactory.load()
    val uuid: String = config.getString("elemental.uuid")
    def toElementalJSON: Json
    def uidPostfix: String
    def messageType: String = "sensor"
  }
  trait ElementalSendable {
    def packets: List[ElemetalDriverable]
  }
}
