package com.noeupapp.middleware.authorizationClient.signUp

import play.api.libs.json.Json


case class ForgotPasswordKey(forgotPasswordToken: String)
case class ForgotPasswordConfig(tokenLength: Int, tokenExpiresInSeconds: Int)

object ForgotPassword {
  implicit val forgotPasswordKeyFormat = Json.format[ForgotPasswordKey]
}
