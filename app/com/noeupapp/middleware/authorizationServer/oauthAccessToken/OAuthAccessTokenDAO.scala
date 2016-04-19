package com.noeupapp.middleware.authorizationServer.oauthAccessToken

import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger
import play.api.db.DB

import scala.language.postfixOps
import scalaz.{-\/, \/-}
import play.api.Play.current

class OAuthAccessTokenDAO {

  val logger = Logger(this.getClass)


  /**
    * Fetch AccessToken by its ID.
    *
    * @param token_id String
    * @return
    */
  def find(token_id: String): Expect[OAuthAccessToken] = {
    Logger.trace("AccessTokens - findAccessTokenDB(" + token_id + ")")
    DB.withConnection({ implicit c =>
      val res = SQL(
        """
          SELECT *
          FROM oauth_access_tokens
          WHERE token = {token}
        """)
        .on(
          "token" -> token_id
        ).as(OAuthAccessToken.accessTokenParser *).headOption
      Logger.trace("AccessTokens - found " + res)
      res match {
        case Some(r) => \/-(r)
        case None => -\/(FailError("Cannot find access token from token"))
      }
    })
  }

  /**
    * Find AccessToken by User and Client
    *
    * @param userId
    * @param clientId
    * @return
    */
  def findByUserAndClient(userId: UUID, clientId: String): Option[OAuthAccessToken] = {
    Logger.trace("getStoredAccessToken - Try to getStoredAccessToken")
    DB.withConnection({ implicit c =>
      SQL(
        """
          SELECT *
          FROM oauth_access_tokens
          WHERE
            client_id = {client_id}::uuid AND
            user_uuid = {user_uuid}::uuid
        """)
        .on(
          "client_id"   -> clientId,
          "user_uuid"   -> userId
        ).as(OAuthAccessToken.accessTokenParser *).headOption
    })
  }

  /**
    * Find Refresh Token by its value
    *
    * @param refreshToken
    * @return
    */
  def findByRefreshToken(refreshToken: String): Option[OAuthAccessToken] = ???
  //accessTokens.where(_.refreshToken === refreshToken).firstOption


  /**
    * Add a new AccessToken
    *
    * @param token
    * @return
    */
  def insert(token: OAuthAccessToken) = {
    logger.warn("...findAuthInfoByRefreshToken :: NOT_IMPLEMENTED")
  }

  // token.id match {
  //   case None => (accessTokens returning accessTokens.map(_.id)) += token
  //   case Some(x) => accessTokens += token
  // }


  /**
    * Update existing AccessToken associated with a user and a client.
    *
    * @param accessToken
    * @param userId
    * @param clientId
    * @return
    */
  def updateByUserAndClient(accessToken: OAuthAccessToken, userId: Int, clientId: String) = ???
  // session.withTransaction {
  //   accessTokens.where(a => a.clientId === clientId && a.userId === userId).delete
  //   accessTokens.insert(accessToken)
  // }


  /**
    * Update AccessToken object based for the ID in accessToken object
    *
    * @param accessToken
    * @return
    */
  def update(accessToken: OAuthAccessToken) = ???
  //accessTokens.where(_.id === accessToken.id).update(accessToken)


  /** DEPRECATED ?
    * Find user from AccessToken
    *
    * @param token
    * @return
    */
  def findUserUUIDFromToken(token: String): Expect[UUID] = {
    DB.withConnection({ implicit c =>
      SQL(
        """
          SELECT user_uuid
          FROM oauth_access_tokens
          WHERE token = {token}
        """)
        .on(
          "token" -> token
        ).as(scalar[UUID].singleOpt) match {
        case None    => -\/(FailError("User not found"))
        case Some(x) => \/-(x)
      }
    })
  }


  def deleteExistingAndCreate(tokenObject: OAuthAccessToken, user_uuid: UUID, client_id: String): OAuthAccessToken = {
    DB.withTransaction({ implicit c => // TODO extract service/DAO

      Logger.trace("AccessTokens - deleteExistingAndCreate")

      val deleted = SQL(
        """
          DELETE FROM oauth_access_tokens
          WHERE
            user_uuid = {user_uuid}::uuid AND
            client_id = {client_id}::uuid
        """)
        .on(
          "client_id" -> client_id,
          "user_uuid" -> user_uuid
        ).executeUpdate()

      Logger.trace("AccessTokens - deleteExistingAndCreate - deleted line number : " + deleted)

      val inserted = SQL(
        """
          INSERT INTO oauth_access_tokens (
            uuid,
            token,
            scope,
            expires_in,
            created_at,
            refresh_token,
            user_uuid,
            client_id
          )
          VALUES (
            {uuid}::uuid,
            {token},
            {scope},
            {expires_in},
            {created_at},
            {refresh_token},
            {user_uuid}::uuid,
            {client_id}::uuid
          )
        """)
        .on(
          "uuid" -> UUID.randomUUID(),
          "token" -> tokenObject.accessToken,
          "scope" -> tokenObject.scope,
          "expires_in" -> tokenObject.expiresIn,
          "created_at" -> tokenObject.createdAt,
          "refresh_token" -> tokenObject.refreshToken,
          "user_uuid" -> user_uuid,
          "client_id" -> client_id
        ).executeUpdate()
      Logger.trace("AccessTokens - deleteExistingAndCreate - inserted line number : " + inserted)
      tokenObject
    })
  }


  def refreshAccessToken(refreshToken: String, newToken: String, createdAt: Date, expiresIn: Long): Boolean = {
    Logger.trace("refreshAccessToken")
    DB.withConnection({ implicit c =>
      SQL(
        """
          UPDATE oauth_access_tokens
          SET
            token         = {token},
            expires_in    = {expires_in},
            created_at    = {created_at}
          WHERE
            refresh_token = {refresh_token}
        """)
        .on(
          "token"         -> newToken,
          "expires_in"    -> expiresIn,
          "created_at"    -> createdAt,
          "refresh_token" -> refreshToken
        ).executeUpdate() == 1
    })
  }

  def refreshToken(refreshToken: String): Option[OAuthAccessToken] = {
    DB.withConnection({ implicit c =>
      SQL(
        """
          SELECT *
          FROM oauth_access_tokens
          WHERE
            refresh_token = {refresh_token}
        """)
        .on(
          "refresh_token" -> refreshToken
        ).as(OAuthAccessToken.accessTokenParser *).headOption
    })
  }

}
