package com.noeupapp.middleware.authorizationServer.authenticator

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.{OAuthAccessTokenDAO, OAuthAccessTokenService}
import com.noeupapp.middleware.entities.user.{UserDAO, UserService}
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

class BearerAuthenticatorDAO(authAccessTokenService: OAuthAccessTokenService,
                             userService: UserService
                            ) extends AuthenticatorDAO[BearerTokenAuthenticator]{

  override def find(id: String): Future[Option[BearerTokenAuthenticator]] = {
    authAccessTokenService.find(id) flatMap {
      case \/-(accessToken) if accessToken.userId.isDefined => {
        Logger.debug(s"BearerAuthenticatorDAO.find($id) -> authAccessTokenService.find -> $accessToken")
        Logger.debug(s"BearerAuthenticatorDAO.find($id) -> userService.findById(${accessToken.userId}) ...")
        userService.findById(accessToken.userId.get).map{
          case \/-(Some(user)) => Some(user)
          case _ => None
        }.map(_.map{user =>
          Logger.debug(s"BearerAuthenticatorDAO.find($id) -> userService.findById(${accessToken.userId}) -> $user")
          val a = BearerTokenAuthenticator(id,
            api.LoginInfo("credentials", user.email.getOrElse("")),
            new DateTime(),
            new DateTime(accessToken.createdAt).plus(accessToken.expiresIn.getOrElse(0l)),
            None
          )
          Logger.info(s"BearerAuthenticatorDAO.find($id) -> $a")
          a
        })
      }

      case \/-(accessToken) if accessToken.userId.isEmpty =>
        Logger.error("accessToken.userId.isEmpty")
        Future.successful(None)

      case -\/(e) =>
        Logger.debug(s"BearerAuthenticatorDAO.find($id) -> authAccessTokenService.find -> nothing found - $e")
        Future.successful(None)
    }
  }

  override def update(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    Logger.warn(s"AuthenticatorDAO.update($authenticator)")
    Future.successful(authenticator)
  }

  override def remove(id: String): Future[Unit] = {
    Logger.warn(s"AuthenticatorDAO.remove")
    authAccessTokenService.deleteByAccessToken(id).map(_ => ())
  }

  override def add(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    Logger.warn(s"AuthenticatorDAO.add($authenticator)")
    Future(authenticator)
  }
}
