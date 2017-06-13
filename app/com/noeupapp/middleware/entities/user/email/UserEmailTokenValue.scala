package com.noeupapp.middleware.entities.user.email

import java.util.UUID

import play.api.libs.json.{JsSuccess, Json}

import scala.language.implicitConversions

/**
  * Created by damien on 13/06/2017.
  */
case class UserEmailTokenValue(userId: UUID, newEmail: String)


object UserEmailTokenValue {

  implicit val userEmailTokenValueFormat = Json.format[UserEmailTokenValue]

  implicit def userEmailTokenValueToString(userEmailTokenValue: UserEmailTokenValue): String =
    Json.stringify(Json.toJson(userEmailTokenValue))

  implicit def fromString(str: String): Option[UserEmailTokenValue] =
    Json.parse(str).validate[UserEmailTokenValue].asOpt

}
