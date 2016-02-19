package qgd.middleware.authorizationClient.controllers

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.i18n.Messages
import play.api.mvc.Request
import qgd.middleware.authorizationClient.controllers.RoleAuthorization.WithRoleAuthorization
import qgd.middleware.authorizationClient.controllers.ScopeAuthorization.WithScopeAuthorization
import qgd.middleware.models.Account

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Manage scope and role authorization
  *
  * @param scopeRoleAuthorization expected scope
  * @param roleAuthorization expected scope
  */
case class ScopeAndRoleAuthorization(
                                   scopeRoleAuthorization: WithScopeAuthorization,
                                   roleAuthorization: WithRoleAuthorization)
  extends Authorization[Account, CookieAuthenticator] {


  /**
    * The account (identity) must be authorized on scope AND on scopes to access API
 *
    * @param identity the user who want to access API
    * @param authenticator the object that stores information about the cookie (data, expiration date, etc.)
    * @param request the request sent by user
    * @param messages used for i18n
    * @tparam B Request type
    * @return true if the user is has the good scope AND the good rôle
    */
  override def isAuthorized[B](identity: Account, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] = {
    for{
      scopeAuthorization <- scopeRoleAuthorization.isAuthorized(identity, authenticator)
      roleAuthorization  <- roleAuthorization.isAuthorized(identity, authenticator)
    } yield scopeAuthorization && roleAuthorization
  }
}


object RoleAuthorization {

  trait WithRoleAuthorization extends Authorization[Account, CookieAuthenticator]

  /**
    * Only allows those users that have at least a service of the selected.
    * Ex: WithService("serviceA", "serviceB") => only users with services "serviceA" OR "serviceB" are allowed.
    */
  case class WithRole(anyOf: String*) extends WithRoleAuthorization {
    def isAuthorized[A](user: Account, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
      WithRole.isAuthorized(user, anyOf: _*)
    }
  }
  object WithRole {
    def isAuthorized(user: Account, anyOf: String*): Boolean =
      anyOf.intersect(user.scopes).nonEmpty
  }

  /**
    * Only allows those users that have every of the selected services.
    * Ex: Restrict("serviceA", "serviceB") => only users with services "serviceA" AND "serviceB" are allowed.
    */
  case class WithRoles(allOf: String*) extends WithRoleAuthorization {
    def isAuthorized[A](user: Account, authenticator: CookieAuthenticator)(implicit r: Request[A], m: Messages) = Future.successful {
      WithRoles.isAuthorized(user, allOf: _*)
    }
  }
  object WithRoles {
    def isAuthorized(user: Account, allOf: String*): Boolean =
      allOf.intersect(user.scopes).size == allOf.size
  }
}


object ScopeAuthorization {

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
}

