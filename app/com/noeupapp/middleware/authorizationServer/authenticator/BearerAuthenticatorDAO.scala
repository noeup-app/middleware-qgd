package com.noeupapp.middleware.authorizationServer.authenticator

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.{OAuthAccessTokenDAO, OAuthAccessTokenService}
import com.noeupapp.middleware.entities.user.{UserDAO, UserService}
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
      case \/-(accessToken) => {
        Logger.debug(s"BearerAuthenticatorDAO.find($id) -> authAccessTokenService.find -> $accessToken")
        userService.findById(accessToken.userId).map{ _.map{ user =>
          Logger.debug(s"BearerAuthenticatorDAO.find($id) -> $user")
          BearerTokenAuthenticator(id,
            api.LoginInfo("credentials", user.email.getOrElse("")),
            new DateTime(),
            new DateTime(accessToken.createdAt).plus(accessToken.expiresIn.getOrElse(0l)),
            None
          )
        }}
      }
      case -\/(e) =>
        Logger.debug(s"BearerAuthenticatorDAO.find($id) -> authAccessTokenService.find -> nothing found - $e")
        Future.successful(None)
    }
  }

  override def update(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    println(s"====> AuthenticatorDAO.update($authenticator)")
    Future.successful(authenticator)
  }

  override def remove(id: String): Future[Unit] = {
    println(s"====> AuthenticatorDAO.remove")
    authAccessTokenService.deleteByAccessToken(id).map(_ => ())
  }

  override def add(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    println(s"====> AuthenticatorDAO.add($authenticator)")
    Future(authenticator)
  }
}
