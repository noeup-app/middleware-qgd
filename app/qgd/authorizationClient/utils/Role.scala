package qgd.authorizationClient.utils

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import qgd.authorizationClient.models.User
import play.api.mvc.Request
import play.api.i18n.Messages
import scala.concurrent.Future

trait WithRoleAuthorization extends Authorization[User, CookieAuthenticator]

/**
  * Only allows those users that have at least a service of the selected.
  * Ex: WithService("serviceA", "serviceB") => only users with services "serviceA" OR "serviceB" are allowed.
  */
case class WithRole(anyOf: String*) extends WithRoleAuthorization{
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithRole.isAuthorized(user, anyOf: _*)
  }
}
object WithRole {
  def isAuthorized(user: User, anyOf: String*): Boolean =
    anyOf.intersect(user.scopes).nonEmpty
}

/**
  * Only allows those users that have every of the selected services.
  * Ex: Restrict("serviceA", "serviceB") => only users with services "serviceA" AND "serviceB" are allowed.
  */
case class WithRoles(allOf: String*) extends WithRoleAuthorization {
  def isAuthorized[A](user: User, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithRoles.isAuthorized(user, allOf: _*)
  }
}
object WithRoles {
  def isAuthorized(user: User, allOf: String*): Boolean =
    allOf.intersect(user.scopes).size == allOf.size
}