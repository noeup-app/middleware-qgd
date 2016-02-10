package qgd.authorizationServer.models

import java.util.{Date, UUID}
import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json
import qgd.errorHandle.FailError
import qgd.errorHandle.FailError.Expect
import scalaoauth2.provider.AccessToken
import scalaz._

case class AccessToken(
                        accessToken: String,
                        refreshToken: Option[String],
                        clientId: String,
                        userUUID: UUID,  // TODO should not be there
                        scope: Option[String],
                        expiresIn: Option[Long],
                        createdAt: Date
                        // TODO missing 'state' value to be returned
                      )

object AccessToken {

  implicit val accessTokensFormat = Json.format[AccessToken]

  val accessToken = {
    get[String]("token_id") ~
    get[Option[String]]("refresh_token") ~
    get[String]("client_id") ~
    get[UUID]("user_uuid") ~
    get[Option[String]]("scope") ~
    get[Option[Long]]("expires_in") ~
    get[Date]("created_at") map {
      case token ~ refreshToken ~ client_id ~ user_uuid ~ scope ~ expirein ~ createdat =>
        AccessToken(token, refreshToken, client_id, user_uuid, scope, expirein, createdat)
    }
  }


  /**
    * Fetch AccessToken by its ID.
    * @param token_id String
    * @return
    */
  def find(token_id: String): Expect[AccessToken] = {
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
        case None => -\/(FailError("Cannot find access tocken from token"))
      }
    })
  }


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


  def findUserNameFromToken(token: String): Expect[String] =  {
    DB.withConnection({ implicit c =>
      SQL(
        """
          SELECT u.cwb_user
          FROM oauth_access_tokens at
          LEFT JOIN cwb_users u ON at.user_uuid = u.uuid
          WHERE token = {token}
        """)
        .on(
          "token" -> token
        ).as(scalar[String].singleOpt) match {
        case None    => -\/(FailError("User not found"))
        case Some(x) => \/-(x)
      }
    })
  }

  def deleteExistingAndCreate(tokenObject: AccessToken, user_uuid: UUID, client_id: String): AccessToken = {
    DB.withConnection({ implicit c =>

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
          "token" -> tokenObject.token,
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



  def getStoredAccessToken(user: UserOauth2, client_id: String): Option[AccessToken] =  {
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
          "client_id"   -> client_id,
          "user_uuid"   -> user.uuid
        ).as(parse *).headOption
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

  def refreshToken(refreshToken: String): Option[AccessToken] = {
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





















//
//import play.api.db.slick.Config.driver.simple._
//import scala.slick.lifted.Tag
//import java.util.Date
//import java.sql.Timestamp
//import oauth2.Crypto
//
//case class AccessToken(id: Option[Int], accessToken: String,
//  refreshToken: String, clientId: String, userId: Int, scope: String,
//  expiresIn: Long, createdAt: java.sql.Timestamp)
//
//class AccessTokens(tag: Tag) extends Table[AccessToken](tag, "access_tokens") {
//  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
//  def accessToken = column[String]("token", O.NotNull)
//  def refreshToken = column[String]("refresh_token", O.NotNull)
//  def clientId = column[String]("client_id", O.NotNull)
//  def userId = column[Int]("user_id")
//  def scope = column[String]("scope", O.NotNull)
//  def expiresIn = column[Long]("expires_in", O.NotNull)
//  def createdAt = column[java.sql.Timestamp]("created_at", O.NotNull)
//  def * = (id.?, accessToken, refreshToken,
//    clientId, userId, scope, expiresIn, createdAt) <>
//    (AccessToken.tupled, AccessToken.unapply _)
//}
//
//object AccessTokens {
//  val accessTokens = TableQuery[AccessTokens]
//
//  /**
//   * Fetch AccessToken by its ID.
//   * @param id
//   * @param session
//   * @return
//   */
//  def get(id: Int)(implicit session: Session): Option[AccessToken] =
//    accessTokens.where(_.id === id).firstOption
//
//  /**
//   * Find AccessToken by token value
//   * @param accessToken
//   * @param session
//   * @return
//   */
//  def find(accessToken: String)(implicit session: Session): Option[AccessToken] =
//    accessTokens.where(_.accessToken === accessToken).firstOption
//
//  /**
//   * Find AccessToken by User and Client
//   * @param userId
//   * @param clientId
//   * @param session
//   * @return
//   */
//  def findByUserAndClient(userId: Int, clientId: String)(implicit session: Session): Option[AccessToken] =
//    accessTokens.where(a => a.userId === userId && a.clientId === clientId).firstOption
//
//  /**
//   * Find Refresh Token by its value
//   * @param refreshToken
//   * @param session
//   * @return
//   */
//  def findByRefreshToken(refreshToken: String)(implicit session: Session): Option[AccessToken] =
//    accessTokens.where(_.refreshToken === refreshToken).firstOption
//
//  /**
//   * Add a new AccessToken
//   * @param token
//   * @param session
//   * @return
//   */
//  def insert(token: AccessToken)(implicit session: Session) = {
//    token.id match {
//      case None => (accessTokens returning accessTokens.map(_.id)) += token
//      case Some(x) => accessTokens += token
//    }
//  }
//
//  /**
//   * Update existing AccessToken associated with a user and a client.
//   * @param accessToken
//   * @param userId
//   * @param clientId
//   * @param session
//   * @return
//   */
//  def updateByUserAndClient(accessToken: AccessToken, userId: Int, clientId: String)(implicit session: Session) = {
//    session.withTransaction {
//      accessTokens.where(a => a.clientId === clientId && a.userId === userId).delete
//      accessTokens.insert(accessToken)
//    }
//  }
//
//  /**
//   * Update AccessToken object based for the ID in accessToken object
//   * @param accessToken
//   * @param session
//   * @return
//   */
//  def update(accessToken: AccessToken)(implicit session: Session) = {
//    accessTokens.where(_.id === accessToken.id).update(accessToken)
//  }
//
//}
//
