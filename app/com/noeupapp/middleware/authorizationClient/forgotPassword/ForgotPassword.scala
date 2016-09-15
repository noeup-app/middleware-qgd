package com.noeupapp.middleware.authorizationClient.forgotPassword

import play.api.libs.json.Json


case class ForgotPasswordKey(forgotPasswordToken: String)
case class ForgotPasswordConfig(url: String, tokenLength: Int, tokenExpiresInSeconds: Int)

object ForgotPassword {
  implicit val forgotPasswordKeyFormat = Json.format[ForgotPasswordKey]
}
