package qgd.authorizationClient.utils

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.mvc.Request
import play.api.i18n.Messages
import qgd.resourceServer.models.Account
import scala.concurrent.Future


trait WithScopeAuthorization extends Authorization[Account, CookieAuthenticator]


trait WithScopeUtils {

  /**
    * For each requiredScope, we check if at least one requiredScope starts with userScope
    *
    * @param requiredScopes the actions required scopes
    * @param userScopes scopes the user has
    * @return a list of boolean of size requiredScopes
    */
  def allRequiredScopesThatStartWithUserScope(requiredScopes: Seq[String], userScopes: List[String]): Seq[Boolean] =
    requiredScopes.map{ requiredScope =>
      userScopes.map(userScope => requiredScope.startsWith(userScope)).fold(false)(_ || _)
    }
}

/**
  * Only allows those users that have AT LEAST ONE correct scopes out of required scopes
  */
case class WithScope(anyOf: String*) extends WithScopeAuthorization{
  def isAuthorized[A](user: Account, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithScope.isAuthorized(user, anyOf: _*)
  }
}
object WithScope extends WithScopeUtils {
  def isAuthorized(user: Account, anyOf: String*): Boolean =
    allRequiredScopesThatStartWithUserScope(anyOf, user.scopes).isEmpty ||
    allRequiredScopesThatStartWithUserScope(anyOf, user.scopes).contains(true)
}

/**
  * Only allows those users that have ALL correct scopes out of required scopes
  */
case class WithScopes(allOf: String*) extends WithScopeAuthorization {
  def isAuthorized[A](user: Account, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
    WithScopes.isAuthorized(user, allOf: _*)
  }
}
object WithScopes extends WithScopeUtils {
  def isAuthorized(user: Account, allOf: String*): Boolean =
    allRequiredScopesThatStartWithUserScope(allOf, user.scopes).isEmpty ||
    allRequiredScopesThatStartWithUserScope(allOf, user.scopes).fold(true)(_ && _)

}