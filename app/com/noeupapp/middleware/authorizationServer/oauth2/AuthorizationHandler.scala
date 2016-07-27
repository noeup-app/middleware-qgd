package com.noeupapp.middleware.authorizationServer.oauth2

import java.util.{Date, NoSuchElementException, UUID}

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.authorizationClient.login.{LoginInfo, PasswordInfoDAO}
import com.noeupapp.middleware.authorizationServer.authCode.{AuthCode, AuthCodeService}
import com.noeupapp.middleware.authorizationServer.client.{Client, ClientService}
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.{OAuthAccessToken, OAuthAccessTokenDAO, OAuthAccessTokenService}
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import com.noeupapp.middleware.utils.{BearerTokenGenerator, Config, NamedLogger, TypeConversion}
import play.api.Logger
import redis.clients.util.Pool
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.Future
import scala.language.implicitConversions
import scalaoauth2.provider._
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import org.joda.time.DateTime

class AuthorizationHandler @Inject() (passwordInfoDAO: PasswordInfoDAO,
                                      accessTokenService: OAuthAccessTokenService,
                                      userService: UserService,
                                      authCodeService: AuthCodeService,
                                      clientService: ClientService)
  extends DataHandler[User] with NamedLogger {

  /**
    * Validate Client if client & secret matches and grantType is in the authorized list // TODO check RFC about refresh token
    *
    * @param request
    * @return
    */
  def validateClient(request: AuthorizationRequest): Future[Boolean] = {
    Logger.debug("AuthorizationHandler.validateClient...")
    val clientCredential = request.clientCredential.get // TODO manage None
    val grantType = request.grantType
    Logger.debug("Client credential: "+clientCredential)
    Logger.debug("grantType: "+ grantType)


    logger.debug(s"given : id = ${clientCredential.clientId} , secret = ${clientCredential.clientSecret}, grantType = $grantType")

    clientService.findByClientIDAndClientSecret(clientCredential.clientId, clientCredential.clientSecret.getOrElse("")) map {
      case -\/(e) =>
        Logger.error(e.toString)
        false
      case \/-(None) =>
        logger.debug(s"Client is not valid")
        false
      case \/-(Some(_)) =>
        logger.debug(s"Client is valid")
        true
    }
  }

  /**
    * Creates a bearer/access token
    *
    * @param authInfo
    */
  def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken]  = {

    Logger.debug("AuthorizationHandler.createAccessToken")

    val refreshToken = Some(BearerTokenGenerator.generateToken)
    //val jsonWebToken = JsonWebTokenGenerator.generateToken(authInfo)
    val accessToken = BearerTokenGenerator.generateToken
    val expiration = Some(Config.OAuth2.accessTokenExpirationInSeconds.toLong)
    val now = new Date()//System.currentTimeMillis())



    val token = new OAuthAccessToken(accessToken, //jsonWebToken,
                                     refreshToken,
                                     authInfo.clientId.getOrElse(""),    // TODO Why does Nulab did use option Here?
                                     Some(authInfo.user.id),
//                                     "Bearer",
                                     authInfo.scope,
                                     expiration,
                                     now)

    //authAccessService.saveAccessToken(token, authInfo) // TODO Check if needed here (does provider really needs to keep token ?)

    accessTokenService.insert(token) map {
      case -\/(e) =>
        Logger.error(s"Error on creating access token $e")
//        e.cause match {
//          case Some(\/-(error)) => Logger.error("AuthorizationHandler.createAccessToken " + error.toString)
//          case Some(-\/(error)) => Logger.error("AuthorizationHandler.createAccessToken " + error.toString)
//          case _ =>
//        }
        throw new Exception("Error on creating access token")
      case \/-(t) => token
    }
  }


  /**
    * // TODO DOC
 *
    * @param authInfo
    * @return
    */
  override def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = {
    Logger.debug("AuthorizationHandler.getStoredAccessToken")
    authInfo.clientId match {
      case Some(clientId) =>
          accessTokenService.findByUserAndClient(authInfo.user.id, clientId).map{
            case -\/(_) => None
            case \/-(res) => Some(res)
          }
      case None => Future.successful(None)
    }
  }

  /**
    * Creates an provider Access Token response from full OauthAccessToken
    *
    * @param accessToken OauthAccessToken
    * @return
    */
  implicit def oauthAccessTokenToAccessToken(accessToken: OAuthAccessToken): AccessToken =
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
  def findUser(request: AuthorizationRequest): Future[Option[User]] = {
    Logger.error("AuthorizationHandler.findUser")
    request match {
      case request: PasswordRequest =>
        Logger.debug(s"AuthorizationHandler.findUser -> PasswordRequest -> ${request.username}")
        userService.validateUser(request.username, request.password) map {
          case \/-(user) => user
          case _ => None
        }
      case request: ClientCredentialsRequest =>
        // Client credential cannot return any user and is just used to provide general information on client
        logger.error("ClientCredentialsRequest : no user defined")
        Future.successful(Some(User.getDefault))
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
  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = {
    {
    Logger.debug("AuthorizationHandler.findAuthInfoByRefreshToken")
    // TODO : this is a fix because userService.findById returns expect[option] instead of expect

    for{
      accessToken <- EitherT(accessTokenService.findByRefreshToken(refreshToken))
      user        <- EitherT(userService.findById(accessToken.userId.get))

    } yield AuthInfo[User](user.get, Some(accessToken.clientId), accessToken.scope, None)
  }.run map {
    case -\/(_) => None
    case \/-(e) => Some(e)
  }}.recover{
    case e: NoSuchElementException =>
      Logger.error(e.getMessage, e)
      None
  }

  /**
    * // TODO DOC
 *
    * @param authInfo
    * @param refreshToken
    * @return
    */
  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = {
    Logger.debug("AuthorizationHandler.refreshAccessToken")
    val newToken = BearerTokenGenerator.generateToken
    val expiresIn = Some(Config.OAuth2.accessTokenExpirationInSeconds.toLong)
    for {
      accessToken    <- EitherT(accessTokenService.findByRefreshToken(refreshToken))
      newAccessToken <- EitherT(accessTokenService.deleteExistingAndCreate(
        accessToken.copy(
          accessToken = newToken,
          expiresIn = expiresIn,
          createdAt = new Date()
        ),
        authInfo.user.id,
        authInfo.clientId.getOrElse(""))
      )
    } yield newAccessToken
  }.run map {
    case -\/(e) =>
      Logger.error(s"AuthorizationHandler $e")
      throw new Exception("An error occurred on refresh token")
    case \/-(r) => r
  }


  // Authorization code grant


  /**
    * // TODO DOC
 *
    * @param code
    * @return
    */

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = {
    authCodeService.find(code) flatMap {
      case \/-(Some(authCode)) if ! authCode.isExpired =>
        userService.findById(authCode.userId).map{
          case \/-(Some(user)) => Some(user)
          case _ => None
        }.map{_.map{user=>

            val authInfo = AuthInfo(user, Some(authCode.clientId), authCode.scope, authCode.redirectUri)
            logger.debug(s"findAuthInfoByCode: $code -> authInfo: $authInfo")
            authInfo
        }}

      case _ => Future.successful(None)
    }
  }


  /**
    * // TODO DOC
 *
    * @param code
    * @return
    */
  def deleteAuthCode(code: String): Future[Unit] = {
    Logger.debug("AuthorizationHandler.deleteAuthCode (TODO)")
    //authAccessService.deleteAuthCode(code)
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
    Logger.debug("AuthorizationHandler.findAccessToken (TODO)")
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
  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = {
    Logger.debug("AuthorizationHandler.findAuthInfoByAccessToken (TODO)")
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