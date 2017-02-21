package com.noeupapp.middleware.authorizationClient.confirmEmail

import com.noeupapp.middleware.entities.user.User
import play.api.libs.json.Json


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

  implicit val confirmEmailFormDataFormat = Json.format[Data]
}
