package com.noeupapp.middleware.authorizationServer.authenticator

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenDAO
import com.noeupapp.middleware.entities.user.{UserDAO, UserService}
import org.joda.time.DateTime

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

class BearerAuthenticatorDAO(authAccessTokenDAO: OAuthAccessTokenDAO,
                                        userService: UserService
                                       ) extends AuthenticatorDAO[BearerTokenAuthenticator]{
  override def find(id: String): Future[Option[BearerTokenAuthenticator]] = {
    authAccessTokenDAO.find(id) match {
      case \/-(accessToken) => {
        userService.findById(accessToken.userId).map{ _.map{ user =>
          BearerTokenAuthenticator(id,
            api.LoginInfo("credentials", user.email.getOrElse("")),
            new DateTime(),
            new DateTime(accessToken.createdAt).plus(accessToken.expiresIn.getOrElse(0l)),
            None
          )
        }}
      }
      case -\/(_) => Future.successful(None)
    }
//    println(s"====> AuthenticatorDAO.find($id)")
//    Future.successful(Some(BearerTokenAuthenticator(id, api.LoginInfo("credentials", "test@test.com"), new DateTime(), new DateTime().plus(5 * 60 * 100), None)))
  }

  override def update(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    println(s"====> AuthenticatorDAO.update($authenticator)")
    Future.successful(authenticator)
  }

  override def remove(id: String): Future[Unit] = {
    println(s"====> AuthenticatorDAO.remove")
    Future(())
  }

  override def add(authenticator: BearerTokenAuthenticator): Future[BearerTokenAuthenticator] = {
    println(s"====> AuthenticatorDAO.add($authenticator)")
    Future(authenticator)
  }
}
