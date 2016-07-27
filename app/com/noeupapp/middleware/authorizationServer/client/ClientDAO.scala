package com.noeupapp.middleware.authorizationServer.client

import java.sql.Connection
import java.util.UUID

import anorm._
import play.api.db.DB

import scala.language.postfixOps

/**
  * Created by damien on 25/07/2016.
  */
class ClientDAO {

  def findAll()(implicit connection: Connection): List[Client] =
    SQL(
      """
          SELECT *
          FROM auth_clients c
      """)
      .as(Client.client *)


  def findByClientId(clientId: String)(implicit connection: Connection) =
    SQL(
      """
          SELECT *
          FROM auth_clients c
          WHERE c.client_id = {client_id}
      """)
      .on("client_id" -> clientId)
      .as(Client.client *).headOption


  def findByClientIDAndClientSecret(clientId: String, clientSecret: String)(implicit connection: Connection): Option[Client] =
    SQL(
      """
          SELECT *
          FROM auth_clients c
          WHERE (c.client_id = {client_id} AND c.client_secret = {client_secret})
      """)
      .on(
        "client_id" -> clientId,
        "client_secret" -> clientSecret
      )
      .as(Client.client *).headOption



  def findByAccessToken(accessToken: String)(implicit connection: Connection): Option[Client] =
    SQL(
      """
        SELECT DISTINCT client.*
        FROM auth_clients client
        INNER JOIN auth_access_tokens accessToken ON accessToken.client_id = client.client_id
        WHERE accessToken.token = {accessToken};
      """)
      .on(
        "accessToken" -> accessToken
      ).as(Client.client *).headOption


  def insert(client: Client)(implicit connection: Connection) =
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


  def update(client: Client)(implicit connection: Connection) =
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


  def delete(clientId: String)(implicit connection: Connection) =
    SQL("""
            DELETE FROM auth_clients
            WHERE client_id = {client_id}
        """)
      .on(
        "client_id" -> clientId
      ).execute()

}
