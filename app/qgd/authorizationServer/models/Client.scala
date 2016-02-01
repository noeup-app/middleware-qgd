package qgd.authorizationServer.models

import java.util.UUID
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json

case class Client(
                   client_id: UUID,
                   client_name: String,
                   client_secret: String,
                   description: String,
                   redirect_uri: String,  // TODO allow several URI
                   scope: String          // TODO change Type to Option[List[String]] or List[String]
                 )

object Client {

  implicit val clientFormat = Json.format[Client]

  val client =
    get[UUID]("client_id") ~
    get[String]("client_name") ~
    get[String]("client_secret") ~
    get[String]("description") ~
    get[String]("redirect_uri") ~
    get[String]("scope") map {
      case id ~ name ~ secret ~ description ~ redirectUri ~ scope => Client(id, name, secret, description, redirectUri, scope)
    }

  /**
    * Fetch a Client by ID
    *
    * @param client_id UUID
    * @return
    */
  def findByClientId(client_id: UUID) = DB.withConnection( implicit c =>
    SQL(
      """
          SELECT *
          FROM auth_clients c
          WHERE c.client_id = "client_id"
      """)
      .on("client_id" -> client_id)
      .as(client *)
  )


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
        "client_id" -> client.client_id,
        "client_name"-> client.client_name ,
        "client_secret" -> client.client_secret,
        "description" -> client.description,
        "redirect_uri" -> client.redirect_uri,
        "scope" -> client.scope
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
        "client_id" -> client.client_id,
        "client_name"-> client.client_name ,
        "client_secret" -> client.client_secret,
        "description" -> client.description,
        "redirect_uri" -> client.redirect_uri,
        "scope" -> client.scope
      ).execute()
    )

}






