package com.noeupapp.middleware.authorizationServer.oauthAccessToken

import java.util.{Date, UUID}

import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
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
case class OAuthAccessToken(
                        accessToken: String,
                        refreshToken: Option[String],
                        clientId: String,
                        // client: Option[Client] = None
                        userId: Option[UUID],
                        // account: Option[OauthIdentity] = None
//                        token_type: String,
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

object OAuthAccessToken {


  implicit val accessTokensFormat = Json.format[OAuthAccessToken]

//  val accessTokenParser = {
//    get[String]("token_id") ~
//    get[Option[String]]("refresh_token") ~
//    get[String]("client_id") ~
//    get[UUID]("user_uuid") ~
//    get[String]("token_type") ~
//    get[Option[String]]("scope") ~
//    get[Option[Long]]("expires_in") ~
//    get[Date]("created_at") map {
//      case token ~ refreshToken ~ clientId ~ userId ~ tokenType ~ scope ~ expireIn ~ createdAt =>
//        OAuthAccessToken(token, refreshToken, clientId, userId, tokenType, scope, expireIn, createdAt)
//    }
//  }

  val accessTokenParser = {
    get[String]("token") ~
    get[Option[String]]("refresh_token") ~
    get[String]("client_id") ~
    get[Option[UUID]]("user_uuid") ~
    get[Option[String]]("scope") ~
    get[Option[Long]]("expires_in") ~
    get[Date]("created_at") map {
      case token ~ refreshToken ~ clientId ~ userId ~ scope ~ expireIn ~ createdAt =>
        OAuthAccessToken(token, refreshToken, clientId, userId, scope, expireIn, createdAt)
    }
  }

}