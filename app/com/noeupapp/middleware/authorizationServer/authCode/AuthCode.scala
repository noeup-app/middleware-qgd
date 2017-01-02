package com.noeupapp.middleware.authorizationServer.authCode

import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import play.api.libs.json.Json



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
  def isExpired: Boolean = {
    val now = new Date().getTime
    val codeTime = createdAt.getTime + expiresIn
    codeTime > now
  }
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


}