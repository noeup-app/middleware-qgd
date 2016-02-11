package qgd.authorizationClient.utils

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.i18n.Messages
import play.api.mvc.Request
import qgd.resourceServer.models.Account
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class ScopeAndRoleAuthorization(
                                   scopeRoleAuthorization: WithScopeAuthorization,
                                   roleAuthorization: WithRoleAuthorization)
  extends Authorization[Account, CookieAuthenticator] {


  override def isAuthorized[B](identity: Account, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] = {
    for{
      scopeAuthorization <- scopeRoleAuthorization.isAuthorized(identity, authenticator)
      roleAuthorization  <- roleAuthorization.isAuthorized(identity, authenticator)
    } yield scopeAuthorization && roleAuthorization
  }
}


