package com.noeupapp.middleware.authorizationClient.login

import com.mohiva.play.silhouette.api.util.Credentials
import play.api.libs.json.Json


/**
  * Credentials to authenticate with. Plus rememberMe information
  *
  * @param identifier the login/email
  * @param password the password
  * @param rememberMe should we remember the user
  */
case class Authenticate(identifier: String, password: String, rememberMe: Boolean){
  def getCredentials = Credentials(identifier, password)
}



object Authenticate {
  implicit val authenticateFormat = Json.format[Authenticate]
}
