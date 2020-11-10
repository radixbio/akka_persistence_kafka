package com.radix.shared.persistence.serializations.device_drivers.messaging

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorRef
import com.radix.shared.persistence.AvroSerializer
import com.radix.shared.persistence.ActorRefSerializer._

object defns {
  sealed trait Request
  final case class SendMessage(id: String, msg: Message) extends Request
  final case class RefreshRecipients() extends Request
  final case class AddRecipient(id: String, params: Map[String, String]) extends Request

  sealed trait Message
  final case class TextMsg(msg: String, title: String, returnResponse: Option[ActorRef[String]]) extends Message
  final case class ButtonMsg(
    msg: String,
    title: String,
    buttons: List[String],
    returnButtonClick: ActorRef[Feedback],
    returnResponse: Option[ActorRef[String]]
  ) extends Message

  sealed trait Feedback
  final case class ButtonClick(btn: String) extends Feedback

  class SendMessageSerializer(implicit as: ExtendedActorSystem) extends AvroSerializer[SendMessage]
  class RefreshRecipientsSerializer extends AvroSerializer[RefreshRecipients]
  class AddRecipientSerializer extends AvroSerializer[AddRecipient]

}
