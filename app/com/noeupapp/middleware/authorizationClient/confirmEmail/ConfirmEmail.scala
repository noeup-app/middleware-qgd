package com.noeupapp.middleware.authorizationClient.confirmEmail

import play.api.libs.json.{Format, Json}


case class ConfirmEmailKey(forgotPasswordToken: String)
case class ConfirmEmailConfig(url: String, tokenLength: Int, tokenExpiresInSeconds: Int)

object ConfirmEmail {
  implicit val confirmEmailKeyFormat: Format[ConfirmEmailKey] = Json.format[ConfirmEmailKey]
}
