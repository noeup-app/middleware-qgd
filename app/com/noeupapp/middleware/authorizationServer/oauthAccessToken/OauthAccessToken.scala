package com.noeupapp.middleware.authorizationServer.oauthAccessToken

import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json
import qgd.middleware.errorHandle.FailError
import qgd.middleware.errorHandle.FailError.Expect
//import scalaoauth2.provider.AccessToken
import scala.language.postfixOps
import scalaz._

/**
  * As described in RFC 6749
  *
  * @param accessToken REQUIRED.  The access token issued by the authorization server.
  * @param refreshToken OPTIONAL.  The refresh token, which can be used to obtain new access tokens using the same authorization grant as described in Section 6.
  * @param token_type REQUIRED.  The type of the token issued. Value is case insensitive.
  * @param scope OPTIONAL, if identical to the scope requested by the client; otherwise, REQUIRED.  The scope of the access token as described by Section 3.3.
  * @param expiresIn RECOMMENDED (but MANDATORY with qgd) The lifetime in seconds of the access token.
  * @param createdAt
  */
case class OauthAccessToken(
                        accessToken: String,
                        refreshToken: Option[String],
                        clientId: String,
                        // client: Option[Client] = None
                        userId: UUID,
                        // account: Option[OauthIdentity] = None
                        token_type: String,
                        scope: Option[String],
                        expiresIn: Option[Long],
                        createdAt: Date
                        // TODO missing 'state' value to be returned
                      ) {
  def isExpired = {
    val codeTime = createdAt.getTime - expiresIn.getOrElse(0).asInstanceOf[Long]
    val now = new Date().getTime
    codeTime < now
  }
}

object OauthAccessToken {

  val logger = Logger(this.getClass)

  implicit val accessTokensFormat = Json.format[OauthAccessToken]

  val accessToken = {
    get[String]("token_id") ~
    get[Option[String]]("refresh_token") ~
    get[String]("client_id") ~
    get[UUID]("user_uuid") ~
    get[String]("token_type") ~
    get[Option[String]]("scope") ~
    get[Option[Long]]("expires_in") ~
    get[Date]("created_at") map {
      case token ~ refreshToken ~ clientId ~ userId ~ tokenType ~ scope ~ expireIn ~ createdAt =>
        OauthAccessToken(token, refreshToken, clientId, userId, tokenType, scope, expireIn, createdAt)
    }
  }

  /**
    * Fetch AccessToken by its ID.
    *
    * @param token_id String
    * @return
    */
  def find(token_id: String): Expect[OauthAccessToken] = {
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
        ).as(accessToken *).headOption
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
  def findByUserAndClient(userId: UUID, clientId: String): Option[OauthAccessToken] = {
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
          ).as(accessToken *).headOption
      })
    }

  /**
   * Find Refresh Token by its value
   *
   * @param refreshToken
   * @return
   */
  def findByRefreshToken(refreshToken: String): Option[OauthAccessToken] = ???
    //accessTokens.where(_.refreshToken === refreshToken).firstOption


  /**
   * Add a new AccessToken
 *
   * @param token
   * @return
   */
  def insert(token: OauthAccessToken) = {
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
  def updateByUserAndClient(accessToken: OauthAccessToken, userId: Int, clientId: String) = ???
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
  def update(accessToken: OauthAccessToken) = ???
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


  def deleteExistingAndCreate(tokenObject: OauthAccessToken, user_uuid: UUID, client_id: String): OauthAccessToken = {
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

  def refreshToken(refreshToken: String): Option[OauthAccessToken] = {
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
        ).as(accessToken *).headOption
    })
  }


}