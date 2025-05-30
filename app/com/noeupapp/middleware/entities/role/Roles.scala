package com.noeupapp.middleware.entities.role

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json

import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The user controller
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 */
class Roles @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, CookieBearerTokenAuthenticator],
                         scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                         roleService: RoleService)
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {


  def fetchByUserId(id: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      roleService.getRolesByUser(id) map {
        case \/-(roles) if roles.isEmpty => NoContent
        case \/-(roles) => Ok(Json.toJson(roles))
        case e @ -\/(_) =>
          Logger.error(e.toString)
          InternalServerError("InternalServerError")
      }
    }

}