package com.noeupapp.middleware.authorizationServer.oauthAccessToken

import java.sql.Connection
import java.util.UUID

import anorm._
import play.api.Logger

import scala.language.postfixOps

class OAuthAccessTokenDAO {


  val logger = Logger(this.getClass)


  /**
    * Fetch AccessToken by its ID.
    *
    * @param token_id String
    * @return
    */
  def find(token_id: String)(implicit connection: Connection): Option[OAuthAccessToken] = {
    Logger.trace(s"OAuthAccessTokenDAO.find($token_id)")
    SQL(
      """
        SELECT *
        FROM auth_access_tokens
        WHERE token = {token}
      """)
      .on(
        "token" -> token_id
      ).as(OAuthAccessToken.accessTokenParser *).headOption
  }

  /**
    * Find AccessToken by User and Client
    *
    * @param userId
    * @param clientId
    * @return
    */
  def findByUserAndClient(userId: UUID, clientId: String)(implicit connection: Connection): Option[OAuthAccessToken] = {
    SQL(
      """
        SELECT *
        FROM auth_access_tokens
        WHERE
          client_id = {client_id} AND
          user_uuid = {user_uuid}::uuid
      """)
      .on(
        "client_id"   -> clientId,
        "user_uuid"   -> userId
      ).as(OAuthAccessToken.accessTokenParser *).headOption
  }

  /**
    * Find Refresh Token by its value
    *
    * @param refreshToken
    * @return
    */
  def findByRefreshToken(refreshToken: String)(implicit connection: Connection): Option[OAuthAccessToken] = {
    SQL(
      """
        SELECT *
        FROM auth_access_tokens
        WHERE
          refresh_token = {refresh_token}
      """)
      .on(
        "refresh_token" -> refreshToken
      ).as(OAuthAccessToken.accessTokenParser *).headOption
  }


  /**
    * Add a new AccessToken
    *
    * @param token
    * @return
    */
  def insert(token: OAuthAccessToken)(implicit connection: Connection): Boolean = {
    SQL(
      """
        INSERT INTO auth_access_tokens (token,
                                        refresh_token,
                                        client_id,
                                        user_uuid,
                                        scope,
                                        expires_in,
                                        created_at)
        VALUES ({token},
                {refresh_token},
                {client_id},
                {user_uuid}::uuid,
                {scope},
                {expires_in},
                {created_at})
      """)
      .on(
        "token" -> token.accessToken,
        "refresh_token" -> token.refreshToken,
        "client_id" -> token.clientId,
        "user_uuid" -> token.userId,
        "scope" -> token.scope,
        "expires_in" -> token.expiresIn,
        "created_at" -> token.createdAt
      ).execute()
  }

  def deleteByRefreshToken(refreshToken: String)(implicit connection: Connection): Boolean = {
    SQL(
      """
          DELETE FROM auth_access_tokens
          WHERE
            refreshToken = {refreshToken}
      """)
      .on(
        "refreshToken" -> refreshToken
      ).execute()
  }

  def deleteByAccessToken(accessToken: String)(implicit connection: Connection): Boolean = {
    SQL(
      """
          DELETE FROM auth_access_tokens
          WHERE
            token = {token}
      """)
      .on(
        "token" -> accessToken
      ).execute()
  }

  def deleteByClientIdAndUserId(client_id: String, user_uuid: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
          DELETE FROM auth_access_tokens
          WHERE
            user_uuid = {user_uuid}::uuid AND
            client_id = {client_id}
      """)
      .on(
        "client_id" -> client_id,
        "user_uuid" -> user_uuid
      ).execute()
  }

}
