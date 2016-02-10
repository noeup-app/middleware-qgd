package qgd.authorizationServer.models

import java.util.{Date, UUID}
import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json

case class AuthCode(
                     authorization_code: String,
                     created_at: Date,
                     client_id: String,
                     expires_in: Long,
                     redirect_uri: Option[String],
                     user_uuid: UUID,
                     used: Boolean
                   ) {
  def isExpired = false
}

object AuthCode {

  implicit val authCodeFormat = Json.format[AuthCode]

  val authcode = {
    get[String]("authorization_code") ~
    get[Date]("created_at") ~
    get[String]("client_id") ~
    get[Long]("expires_in") ~
    get[Option[String]]("redirect_uri") ~
    get[UUID]("user_uuid") ~
    get[Boolean]("used") map {
      case authorization_code ~ created_at ~ client_id ~ expires_in ~ redirect_uri ~ user_uuid ~ used =>
        AuthCode(authorization_code, created_at, client_id, expires_in, redirect_uri, user_uuid, used)
    }
  }

  /**
    * add a new AuthCode in DB.
    * @param code String
    * @param clientId UUID
    * @param createdAt DateTime
    * @param expiresIn Integer (time in milliseconds)
    * @param state String
    * @param redirect_uri String
    * @param user_uuid UUID
    * @return
    */
  def insert(code: String, clientId: String, createdAt: Date, expiresIn: Int, state: Option[String], redirect_uri: Option[String], user_uuid: UUID) = {
    DB.withConnection({ implicit c =>
      SQL(
        """
          INSERT INTO oauth_auth_codes (
            authorization_code,
            expires_in,
            created_at,
            client_id,
            redirect_uri,
            user_uuid,
            used
          )
          VALUES (
            {authorization_code},
            {expires_in},
            {created_at},
            {client_id},
            {redirect_uri},
            {user_uuid}::uuid,
            {used}
          )
        """)
        .on(
          "authorization_code" -> code,
          "expires_in"         -> expiresIn,
          "created_at"         -> createdAt,
          "client_id"          -> clientId,
          "redirect_uri"       -> redirect_uri,
          "user_uuid"          -> user_uuid,
          "used"               -> false
        ).execute()
    })
  }

  def setAuthCodeAsUsed(code: String, used: Boolean = true) = DB.withConnection(implicit c =>
    SQL(
      """
         UPDATE oauth_auth_codes
         SET
           used = {used}
         WHERE
           authorization_code = {code}
      """)
      .on(
        "code" -> code,
        "used" -> used
      ).execute()
  )


  def find(code: String): Option[AuthCode] = DB.withConnection(implicit c =>
      SQL(
        """
           SELECT *
           FROM oauth_auth_codes
           WHERE
            authorization_code = {code} AND
            used = false
        """)
        .on(
          "code" -> code
        ).as(authcode *).headOption
  )

}
//package qgd.authorizationServer.models
//
//import play.api.db.slick.Config.driver.simple._
//import scala.slick.lifted.Tag
//import java.util.Date
//import java.sql.Timestamp
//import oauth2.Crypto
//
//case class AuthCode(authorizationCode: String, userId: Int,
//  redirectUri: Option[String], createdAt: java.sql.Timestamp,
//  scope: Option[String], clientId: String, expiresIn: Int)
//
//class AuthCodes(tag: Tag) extends Table[AuthCode](tag, "auth_codes") {
//  def authorizationCode = column[String]("authorization_code", O.PrimaryKey)
//  def userId = column[Int]("user_id")
//  def redirectUri = column[Option[String]]("redirect_uri")
//  def createdAt = column[java.sql.Timestamp]("created_at")
//  def scope = column[Option[String]]("scope")
//  def clientId = column[String]("client_id")
//  def expiresIn = column[Int]("expires_in")
//  def * = (authorizationCode, userId, redirectUri, createdAt, scope,
//    clientId, expiresIn) <>
//    (AuthCode.tupled, AuthCode.unapply _)
//}
//
//object AuthCodes {
//  val authCodes = TableQuery[AuthCodes]
//  val log = play.Logger.of("application")
//
//  /**
//   * Add AuthCode object to database.
//   * @param ac
//   * @param session
//   * @return
//   */
//  def insert(ac: AuthCode)(implicit session: Session) = {
//    authCodes += ac
//  }
//
//  /**
//   * Delete AuthCode object from database.
//   * @param ac
//   * @param session
//   * @return
//   */
//  def delete(ac: AuthCode)(implicit session: Session) =
//    authCodes.where(_.clientId === ac.clientId).delete
//
//  /**
//   * Find AuthCode object by its value.
//   * @param authCode
//   * @param session
//   * @return
//   */
//  def find(authCode: String)(implicit session: Session) = {
//
//    val code = authCodes.where(_.authorizationCode === authCode).firstOption
//
//    log.debug(code.toString())
//    // filtering out expired authorization codes
//    code.filter { p =>
//      val codeTime = p.createdAt.getTime + (p.expiresIn)
//      val currentTime = new Date().getTime
//      log.debug(s"codeTime: $codeTime, currentTime: $currentTime")
//      codeTime > currentTime
//    }
//  }
//
//  /**
//   * Generate a new AuthCode for given client and other details.
//   * @param clientId
//   * @param redirectUri
//   * @param scope
//   * @param userId
//   * @param expiresIn
//   * @param session
//   * @return
//   */
//  def generateAuthCodeForClient(clientId: String, redirectUri: String,
//    scope: String, userId: Int, expiresIn: Int)(
//      implicit session: Session): Option[AuthCode] = {
//
//    Client.findByClientId(clientId).map {
//      client =>
//        {
//          val authCode = Crypto.generateAuthCode()
//          val createdAt = new Timestamp(new Date().getTime)
//          val redirectUriOpt = Some(redirectUri)
//          val scopeOpt = Some(scope)
//          val ac = AuthCode(authCode, userId, redirectUriOpt,
//            createdAt, scopeOpt, clientId, expiresIn)
//
//          // replace with new auth code
//          delete(ac)
//          insert(ac)
//          ac
//        }
//    }
//  }
//}