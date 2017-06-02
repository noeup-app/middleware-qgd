package com.noeupapp.middleware.notifications

import java.util.UUID

import com.noeupapp.middleware.entities.user.User
import play.api.libs.json.{Json, Writes}

import scala.language.implicitConversions

/**
  * Created by damien on 30/05/2017.
  */
case class NotificationMessage(notificationId: UUID, userId: UUID, message_type: String, message_data: String)


object NotificationMessage {


  implicit def webSocketMessageFormatToJson[T](notifMessage: NotificationMessage): String =
    Json.stringify(Json.obj(
      "id" -> notifMessage.notificationId,
      "message_type" -> notifMessage.message_type,
      "message_data" -> notifMessage.message_data
    ))


}
