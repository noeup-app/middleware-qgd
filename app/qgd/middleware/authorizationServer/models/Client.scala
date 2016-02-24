package qgd.middleware.authorizationServer.models

import java.util.UUID
import scala.language.postfixOps
import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db.DB
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
    get[Option[String]]("authorized_grant_types") ~
    get[String]("description") ~
    get[String]("redirect_uri") ~
    get[Option[String]]("default_scope") map {
      case id ~ name ~ secret ~ authorizedGrantTypes ~ description ~ redirectUri ~ defaultScope =>
        Client(id, name, secret, authorizedGrantTypes, description, redirectUri, defaultScope)
    }


  /**
    * Fetch a Client by ID
    *
    * @param clientId String
    * @return
    */
  def findByClientId(clientId: String) = DB.withConnection( implicit c =>
    SQL(
      """
          SELECT *
          FROM auth_clients c
          WHERE c.client_id = {client_id}
      """)
      .on("client_id" -> clientId)
      .as(client *).headOption
  )

  def validateClient(clientId: String, clientSecret: Option[String]=None, grantType: String): Boolean = { DB.withConnection( implicit c =>
    SQL(
      """
          SELECT *
          FROM auth_clients c
          WHERE (c.client_id = {client_id} AND c.client_secret = {client_secret})
      """)
      .on("client_id" -> clientId,
          "client_secret" -> clientSecret,
          "authorizedGrantTypes" -> grantType)
      .as(client *)
      .headOption.flatMap(getGrantTypesToList(_)).contains(grantType)
  )}


  /**
    * Transform space separated grant type values to list
    *
    * @param client Defined client
    * @return List of authorized grant types
    */
  def getGrantTypesToList(client: Client) = client.authorizedGrantTypes.map(_.split(" "))


  /**
    * Transform space separated scope values to list
    *
    * @param client Defined client
    * @return List of default scope for a given client
    */
  def getDefaultScopeToList(client: Client) = client.defaultScope.map(_.split(" "))


  /**
    * Fetch all clients
    *
    * @return List[Client]
    */
    def list() = DB.withConnection( implicit c =>
      SQL(
        """
          SELECT *
          FROM auth_clients c
        """)
        .as(client *)
  )


  /**
    * Add a new Client to database.
    *
    * @param client
    */
    def insert(client: Client) = DB.withConnection( implicit c =>
      SQL("""
          INSERT INTO auth_clients c
            (client_id, client_name, client_secret, description, redirect_uri, scope)
          VALUES
            ({client_id}, {client_name}, {client_secret}, {description}, {redirect_uri}, {scope})
        """)
      .on(
        "client_id" -> client.clientId,
        "client_name"-> client.clientName ,
        "client_secret" -> client.clientSecret,
        "description" -> client.description,
        "redirect_uri" -> client.redirect_uri,
        "scope" -> client.defaultScope
      ).execute()
    )


  /**
    * Delete the the given client from database.
    *
    * @param client_id UUID
    * @return
    */
    def delete(client_id: UUID) = DB.withConnection( implicit c =>
      SQL("""
            DELETE FROM auth_clients
            WHERE client_id = {client_id}
          """)
        .on(
          "client_id" -> client_id
        ).execute()
      )


  /**
    * Update the given client.
    *
    * @param client Client
    * @return
    */
    def update(client: Client) =  DB.withConnection( implicit c =>
      SQL("""
            UPDATE auth_clients
            SET client_id = {client_id},
                client_name = {client_name},
                client_secret = {client_secret},
                description = {description},
                redirect_uri = {redirect_uri},
                scope = {scope}
            WHERE client_id = {client_id}
          """)
      .on(
        "client_id" -> client.clientId,
        "client_name"-> client.clientName ,
        "client_secret" -> client.clientSecret,
        "description" -> client.description,
        "redirect_uri" -> client.redirect_uri,
        "scope" -> client.defaultScope
      ).execute()
    )


}






