package com.noeupapp.middleware.webSockets

import play.api.libs.json.{Json, Writes}

import scala.language.implicitConversions

/**
  * Created by damien on 30/05/2017.
  */
case class WebSocketMessage[T](message_type: String, message_data: T)


object WebSocketMessage {

  val forbidden = WebSocketMessage[String]("Forbidden", "Forbidden")


  implicit def webSocketMessageFormatToJson[T](webSocketMessage: WebSocketMessage[T])(implicit w: Writes[T]): String =
    Json.stringify(Json.obj(
      "message_type" -> webSocketMessage.message_type,
      "message_data" -> webSocketMessage.message_data
    ))


}
