package com.noeupapp.middleware.notifications

import com.noeupapp.middleware.entities.user.User
import play.api.libs.json.{Json, Writes}

import scala.language.implicitConversions

/**
  * Created by damien on 30/05/2017.
  */

case class NotificationMessage[T](user: User, message_type: String, message_data: T)


object NotificationMessage {


  implicit def webSocketMessageFormatToJson[T](notifMessage: NotificationMessage[T])(implicit w: Writes[T]): String =
    Json.stringify(Json.obj(
      "message_type" -> notifMessage.message_type,
      "message_data" -> notifMessage.message_data
    ))


}
