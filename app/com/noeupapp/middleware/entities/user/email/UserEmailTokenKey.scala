package com.noeupapp.middleware.entities.user.email

import play.api.libs.json.Json

import scala.language.implicitConversions

/**
  * Created by damien on 13/06/2017.
  */
case class UserEmailTokenKey(userEmailToken: String)

object UserEmailTokenKey {

  implicit val userEmailTokenKeyFormat = Json.format[UserEmailTokenKey]

  implicit def userEmailTokenKeyToString(userEmailTokenKey: UserEmailTokenKey): String =
    Json.stringify(Json.toJson(userEmailTokenKey))

}
