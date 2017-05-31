package com.noeupapp.middleware.notifications

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by damien on 31/05/2017.
  */
class RedisNotificationKeyFactory {

  def createKey(userId: UUID): String = Json.stringify(Json.obj(
    "notification" -> userId
  ))

}
