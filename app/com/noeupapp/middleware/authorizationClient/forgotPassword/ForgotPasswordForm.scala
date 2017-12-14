package com.noeupapp.middleware.authorizationClient.forgotPassword

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * The form which handles the sign up process.
 */
object ForgotPasswordForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "email" -> email
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   * @param email The email of the user.
   */
  case class Data(email: String)

  implicit val forgotPasswordFormDataFormat = Json.format[Data]
}
