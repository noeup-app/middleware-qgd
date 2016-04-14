package com.noeupapp.middleware.authorizationServer.authCode

import java.sql.Timestamp
import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import com.noeupapp.middleware.authorizationServer.client.Client
import com.noeupapp.middleware.utils.AuthCodeGenerator
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json

import scala.language.postfixOps


case class AuthCode(
                     authorizationCode: String,
                     createdAt: Date,
                     clientId: String,
                     scope: Option[String],
                     expiresIn: Long,
                     redirectUri: Option[String],
                     userId: UUID,
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
    get[Option[String]]("scope") ~
    get[Long]("expires_in") ~
    get[Option[String]]("redirect_uri") ~
    get[UUID]("user_uuid") ~
    get[Boolean]("used") map {
      case authorization_code ~ created_at ~ client_id ~ scope ~ expires_in ~ redirect_uri ~ user_uuid ~ used =>
        AuthCode(authorization_code, created_at, client_id, scope, expires_in, redirect_uri, user_uuid, used)
    }
  }

  /**
    * add a new AuthCode in DB.
 *
    * @param code AuthCode
    * @return
    */
  def insert(code: AuthCode) = {
    DB.withConnection({ implicit c =>
      SQL(
        """
          INSERT INTO auth_auth_codes (
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
          "authorization_code" -> code.authorizationCode,
          "expires_in"         -> code.expiresIn,
          "created_at"         -> code.createdAt,
          "client_id"          -> code.clientId,
          //"scope"              -> code.scope,
          "redirect_uri"       -> code.redirectUri,
          "user_uuid"          -> code.userId,
          "used"               -> false
        ).execute()
    })
  }

  def setAuthCodeAsUsed(code: String, used: Boolean = true) = DB.withConnection(implicit c =>
    SQL(
      """
         UPDATE auth_auth_codes
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
           FROM auth_auth_codes
           WHERE
            authorization_code = {code}
            used = false
        """)
        .on(
          "code" -> code
        ).as(authcode *).headOption
  )


  /**
    * Generate a new AuthCode for given client and other details.
    *
    * @param clientId
    * @param redirectUri
    * @param scope
    * @param userId
    * @param expiresIn
    * @return
    */
  def generateAuthCodeForClient(clientId: String, redirectUri: String, scope: String, userId: UUID, expiresIn: Int): Option[AuthCode] = {

    Client.findByClientId(clientId).map {
      client =>
        val code = AuthCodeGenerator.generateAuthCode()
        val createdAt = new Timestamp(new Date().getTime)
        val authCode = AuthCode(
                                code,
                                createdAt,
                                clientId,
                                Some(scope),
                                expiresIn.toLong,
                                Some(redirectUri),
                                userId,
                                used = false)

        // replace with new auth code
        setAuthCodeAsUsed(authCode.authorizationCode)
        insert(authCode)
        authCode // TODO MANAGE ERROR and USE WITH TRANSACTION
    }
  }

}