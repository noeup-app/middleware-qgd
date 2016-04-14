package qgd.middleware.authorizationServer
package handlers

import org.joda.time.DateTime
import qgd.middleware.utils.NamedLogger
import scala.language.implicitConversions
import scalaoauth2.provider
import qgd.middleware.authorizationServer.utils.{Config, BearerTokenGenerator}
import scalaoauth2.provider._
import controllers.Clients
import models.{ Client, AuthCode, OauthAccessToken}
import qgd.middleware.models.Account
import java.util.{Date, UUID}
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}

class AuthorizationHandler extends DataHandler[Account] with NamedLogger {

  /**
    * Validate Client if client & secret matches and grantType is in the authorized list // TODO check RFC about refresh token
    *
    * @param request
    * @return
    */
  def validateClient(request: AuthorizationRequest): Future[Boolean] = Future.successful {
    val clientCredential = request.clientCredential.get // TODO manage None
    val grantType = request.grantType

    logger.debug("validating client ...")
    logger.debug("given:")
    logger.debug(s"id = ${clientCredential.clientId} , secret = ${clientCredential.clientSecret}, grantType = $grantType")

    val isValid = Client.validateClient(clientCredential.clientId, clientCredential.clientSecret, grantType)

    logger.debug(s"Client isValid : $isValid")
    isValid
  }


  /**
    * Creates a bearer/access token
    *
    * @param authInfo
    */
  def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken]  = {

    val refreshToken = Some(BearerTokenGenerator.generateToken)
    //val jsonWebToken = JsonWebTokenGenerator.generateToken(authInfo)
    val accessToken = BearerTokenGenerator.generateToken
    val expiration = Some(Config.OAuth2.accessTokenExpirationInSeconds.toLong)
    val now = new Date()//System.currentTimeMillis())

    val token = new OauthAccessToken( accessToken, //jsonWebToken,
                                      refreshToken,
                                      authInfo.clientId.getOrElse(""),    // TODO Why does Nulab did use option Here?
                                      authInfo.user.id,
                                      "Bearer",
                                      authInfo.scope,
                                      expiration,
                                      now
                                    )

    //authAccessService.saveAccessToken(token, authInfo) // TODO Check if needed here (does provider really needs to keep token ?)
    OauthAccessToken.insert(token)

    logger.debug(s"...create access Token: $token")

    Future.successful(token)
  }


  /**
    * // TODO DOC
 *
    * @param authInfo
    * @return
    */
  override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = ???

  /**
    * Creates an provider Access Token response from full OauthAccessToken
    *
    * @param accessToken OauthAccessToken
    * @return
    */
  implicit def oauthAccessTokenToAccessToken(accessToken: OauthAccessToken): AccessToken =
    AccessToken(
      accessToken.accessToken,
      accessToken.refreshToken,
      None,
      accessToken.expiresIn,
      accessToken.createdAt
    )



  // Password grant


  /**
    * Find User Account from Resources server account  // TODO Account and Account should be the same
    *
    * @param request
    * @return
    */
  def findUser(request: AuthorizationRequest): Future[Option[Account]] = {
    request match {
      case request: PasswordRequest =>
        Future.successful(Account.findByEmailAndPassword(request.username, request.password))
      case request: ClientCredentialsRequest =>
        // Client credential cannot return any user and is just used to provide general information on client
        logger.debug("ClientCredentialsRequest : no user defined")
        Future.successful(None)
      case _ =>
        logger.warn("Unauthorized request grant type")
        Future.successful(None)
    }
  }


  // Refresh token grant


  /**
    * // TODO DOC
 *
    * @param refreshToken
    * @return
    */
  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
    logger.warn("...findAuthInfoByRefreshToken :: NOT_IMPLEMENTED")
    Future.successful(None)
  }

  /**
    * // TODO DOC
 *
    * @param authInfo
    * @param refreshToken
    * @return
    */
  def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = {
    // TODO GUILLAUME : refresh != create
    createAccessToken(authInfo)
  }


  // Authorization code grant


  /**
    * // TODO DOC
 *
    * @param code
    * @return
    */
  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = Future.successful{ // TODO MANAGE FUTURE IN SERVICE
  val storedCode = AuthCode.find(code)
    val now = new Date().getTime

    // filter out expired code
    storedCode.filter { c =>
      val codeTime = c.createdAt.getTime + c.expiresIn
      logger.debug(s"codeTime: $codeTime, currentTime: $now")
      codeTime > now
    }.flatMap { c =>
      logger.debug("valid code found!")
      Account.findByUserId(c.userId).map { user =>
        val authInfo = AuthInfo(user, Some(c.clientId), c.scope, c.redirectUri)
        logger.debug(s"findAuthInfoByCode: $code -> authInfo: $authInfo")
        authInfo
      }
    }
  }

  /**
    * // TODO DOC
 *
    * @param code
    * @return
    */
  def deleteAuthCode(code: String): Future[Unit] = {
    //authAccessService.deleteAuthCode(code)
    logger.warn("...deleteAuthCode :: NOT_IMPLEMENTED")
    Future.successful(None)
  }

  // Protected resource


  /**
    * // TODO DOC
 *
    * @param token
    * @return
    */
  def findAccessToken(token: String): Future[Option[AccessToken]] = {
    logger.warn("...findAccessToken :: NOT_IMPLEMENTED")
    Future.successful(None)
    /*    logger.debug(s"findAccessToken: $token")
        //authAccessService.findAccessToken(token)
        qgd.authorizationServer.models.AccessToken.find(token) match{
          case \/-(t) => Some(t)
          case -\/(f) => None // TODO send error to event bus
        }*/
  }

  /**
    * // TODO DOC
 *
    * @param accessToken
    * @return
    */
  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = {
    logger.warn("...findAuthInfoByAccessToken :: NOT_IMPLEMENTED")
    Future.successful(None)
    /*// TODO MANAGE FUTURE IN HIGHER LEVEL!
    logger.debug(s"findAuthInfoByAccessToken: $accessToken")
    //authAccessService.findAuthInfoByAccessToken(accessToken.token)
    val storedToken = AccessToken.findUserUUIDFromToken(accessToken.accessToken)
    val now = new Date().getTime

    // filter out expired code
    accessToken.isExpired match {
      case true =>
        logger.debug(s"findAuthInfoByAccessToken: $accessToken -> Expired")
        None
      case false =>
        models.Account.findByUserId(accessToken.userId).map { user =>
          val authInfo = AuthInfo(user, Some(accessToken.clientId), accessToken.scope, None)
          logger.debug(s"findAuthInfoByAccessToken: $accessToken -> authInfo: $authInfo")
          authInfo
        }
    }*/
  }

}