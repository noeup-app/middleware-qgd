package com.noeupapp.middleware.authorizationServer.client

import anorm.SqlParser._
import anorm._
import play.api.libs.json.Json



/**
  * // TODO DOC
  *
  * @param clientId Ramdom string
  * @param clientName Client application name
  * @param clientSecret Ramdom string
  * @param authorizedGrantTypes list as comma separated value. Default : None
  * @param description Optional client details or description
  * @param redirect_uri List as comma separated value (default redirect URI is the first one)
  * @param defaultScope Optional list (space separated value witch order does not matter). Default : None
  */
case class Client(
                   clientId: String,
                   clientName: String,
                   clientSecret: String,
                   authorizedGrantTypes: Option[String] = None,
                   description: String,
                   redirect_uri: String,                    // TODO Should permit to set several uris
                   defaultScope: Option[String] = None
                 )

object Client {

  implicit val clientFormat = Json.format[Client]

  val client =
    get[String]("client_id") ~
    get[String]("client_name") ~
    get[String]("client_secret") ~
    //get[Option[String]]("authorized_grant_types") ~
    get[String]("description") ~
    get[String]("redirect_uri") /*~ get[Option[String]]("default_scope")*/ map {
      case id ~ name ~ secret /*~ authorizedGrantTypes*/ ~ description ~ redirectUri /*~ defaultScope*/ =>
        Client(id, name, secret, None, description, redirectUri)
    }

}






