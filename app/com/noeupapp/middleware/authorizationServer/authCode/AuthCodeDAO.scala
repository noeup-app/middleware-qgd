package com.noeupapp.middleware.authorizationServer.authCode

import java.sql.{Connection, Timestamp}
import java.util.{Date, UUID}

import anorm._
import com.noeupapp.middleware.authorizationServer.client.Client
import com.noeupapp.middleware.utils.AuthCodeGenerator
import play.api.db.DB

class AuthCodeDAO {


  /**
    * add a new AuthCode in DB.
    *
    * @param code AuthCode
    * @return
    */
  def insert(code: AuthCode)(implicit connection: Connection) = {
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
  }

  // TODO : replace with full update
  def setAuthCodeAsUsed(code: String, used: Boolean = true)(implicit connection: Connection) =
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



  def find(code: String)(implicit connection: Connection): Option[AuthCode] =
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
      ).as(AuthCode.authcode.singleOpt)



}
