package qgd.authorizationClient.utils

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import qgd.authorizationClient.models.User
import play.api.mvc.Request
import play.api.i18n.Messages
import scala.concurrent.Future

/**
  * Only allows those users that have at least a service of the selected.
  * Master service is always allowed.
  * Ex: WithService("serviceA", "serviceB") => only users with services "serviceA" OR "serviceB" (or "master") are allowed.
  */
case class WithScope(anyOf: String*) extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithScope.isAuthorized(user, anyOf: _*)
  }
}
object WithScope {
  def isAuthorized(user: User, anyOf: String*): Boolean =
    anyOf.intersect(user.scopes).nonEmpty || user.scopes.contains("master")
}

/**
  * Only allows those users that have every of the selected services.
  * Master service is always allowed.
  * Ex: Restrict("serviceA", "serviceB") => only users with services "serviceA" AND "serviceB" (or "master") are allowed.
  */
case class WithScopes(allOf: String*) extends Authorization[User, CookieAuthenticator] {
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithScopes.isAuthorized(user, allOf: _*)
  }
}
object WithScopes {
  def isAuthorized(user: User, allOf: String*): Boolean =
    allOf.intersect(user.scopes).size == allOf.size || user.scopes.contains("master")
}