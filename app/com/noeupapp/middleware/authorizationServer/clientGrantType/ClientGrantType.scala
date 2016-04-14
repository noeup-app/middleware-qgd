package com.noeupapp.middleware.authorizationServer.clientGrantType

import java.util.UUID

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json
import GrantType.GrantType

import scala.language.postfixOps

case class ClientGrantType(
                            clientId: String,
                            grantType: GrantType
                          )

object ClientGrantType {

  implicit val clientGrantTypeFormat = Json.format[ClientGrantType]

  val clientGrantType =
    get[String]("client_Id") ~
    get[String]("grantType") map {
      case id ~ grantType => ClientGrantType(id, GrantType.fromString(grantType))
    }

  /**
    * Check if the given client and secret have a GrantType access configured in database.
    *
    * @param clientId UUID
    * @param clientSecret String
    * @param grantType String
    * @return Boolean
    */
  def validate(clientId: UUID, clientSecret: String, grantType: String): Boolean = DB.withConnection( implicit c =>
    SQL(
      """
          SELECT count(*)
          FROM auth_clients c
          INNER JOIN auth_client_grant_types rcg ON rcg.client_id = c.client_id
          WHERE c.client_secret = {client_secret} AND c.client_id = {client_id}::uuid AND rcg.grantType = {grantType}
      """)
      .on(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "grantType" -> grantType
      ).as(scalar[Int].single) == 1
  )


  /**
    * Fetch all grants by client
    *
    * @return List[GrantType]
    */
  def list(clientId: String) = DB.withConnection( implicit c =>
    SQL(
      """
          SELECT *
          FROM auth_client_grant_types acgt
          WHERE acgt.client_id = {client_id}
      """)
      .on(
        "client_id" -> clientId
      ).as(clientGrantType *)
  )


  /**
    * allow new grant type to client.
    *
    * @param clientId String
    * @param grantType GrantType
    */
  def insert(clientId: String, grantType: GrantType) = DB.withConnection( implicit c =>
    SQL("""
          INSERT INTO auth_client_grant_types
            (client_id, grant_type)
          VALUES
            ({client_id}, {grant_type})
        """)
      .on(
        "client_id" -> clientId,
        "grant_type" -> grantType.toString // TODO TO BE DISCUSSED : Is it a good idea to use enum in pg?
      ).execute()
  )


  /**
    * Remove given grant for client from database.
    *
    * @param clientId UUID
    * @return
    */
  def delete(clientId: String, grantType: GrantType) = DB.withConnection( implicit c =>
    SQL("""
            DELETE FROM auth_client_grant_types acgt
            WHERE client_id = {client_id} AND grantType = {grantType}
        """)
      .on(
        "client_id" -> clientId
        //"grantType" -> grantType
      ).execute()
  )

}