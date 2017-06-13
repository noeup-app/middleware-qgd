package com.noeupapp.middleware.entities.user.email

import play.api.libs.json.Json

/**
  * Created by damien on 06/06/2017.
  */
case class UserEmail(email: String)


object UserEmail {

  implicit val userEmailFormat = Json.format[UserEmail]

}
