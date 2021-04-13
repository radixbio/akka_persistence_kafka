package com.radix.shared.persistence.serializations.device_drivers.elemental_radix_driver

import akka.actor.typed.ActorRef
import com.typesafe.config.ConfigFactory
import io.circe.Json

object ElementalRadixDriverTypes {
  trait ElementalDriverable {
    val uuid: String = ConfigFactory.load().getString("elemental.uuid")
    def toElementalJSON: Json
    def uidPostfix: String
    def messageType: String = "sensor"
  }
  trait ElementalSendable {
    def packets: List[ElementalDriverable]
  }

  object SendToElemental {
    def apply(data:ElementalDriverable, replyTo:Option[ActorRef[ElementalResponse]]): SendToElemental = {
      new SendToElemental(List(data), replyTo)
    }

    def apply(data:ElementalSendable, replyTo:ActorRef[ElementalResponse]): SendToElemental = {
      new SendToElemental(data.packets, Some(replyTo))
    }

    def apply(data:ElementalDriverable): SendToElemental = {
      new SendToElemental(List(data), None)
    }

    def apply(data:ElementalSendable): SendToElemental = {
      new SendToElemental(data.packets, None)
    }
  }
  case class SendToElemental(packets: List[ElementalDriverable], replyTo:Option[ActorRef[ElementalResponse]])
    extends ElementalSendable {

    def reply(msg:ElementalResponse): Unit = replyTo match {
      case None => // Ignore
      case Some(value) => value ! msg
    }
  }
  case class ElementalResponse(data:String)
}