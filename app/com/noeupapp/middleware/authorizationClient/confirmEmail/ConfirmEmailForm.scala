package com.noeupapp.middleware.authorizationClient.confirmEmail

import com.noeupapp.middleware.entities.user.User
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.libs.json.Json

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by vincent on 20/02/17.
  */
object ConfirmEmailForm {

  /**
    * The form data.
    *
    * @param message Message send
    * @param user The user send.
    */
  case class Data(message: String, user: User)


  /**
    * Form data to resend an email confirmation
    * @param email email targeted
    */
  case class Resending(email: String)

  val resendingForm = Form(
    mapping(
      "email"       -> email
    )(Resending.apply)(Resending.unapply)
  )

  implicit val resendingFormDataFormat = Json.format[Resending]
}