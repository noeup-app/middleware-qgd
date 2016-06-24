package com.noeupapp.middleware.authorizationClient.signUp

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * The form which handles the sign up process.
 */
object ForgotPasswordAskNewPasswordForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "password" -> nonEmptyText,
      "passwordConfirm" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(password: String, passwordConfirm: String){
    lazy val arePasswordsEqual = password.equals(passwordConfirm)
  }

  implicit val forgotPasswordFormDataFormat = Json.format[Data]
}
