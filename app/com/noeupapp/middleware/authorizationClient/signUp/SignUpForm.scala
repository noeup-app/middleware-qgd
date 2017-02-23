package com.noeupapp.middleware.authorizationClient.signUp

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * The form which handles the sign up process.
 */
object SignUpForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText,
      "ownedBy" -> optional(text)
    )(Data.apply)(Data.unapply)
  )

  /**
   * The form data.
   *
   * @param firstName The first name of a user.
   * @param lastName The last name of a user.
   * @param email The email of the user.
   * @param password The password of the user.
   */
  case class Data(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    ownedBy: Option[String])

  implicit val signUpFormDataFormat = Json.format[Data]
}
