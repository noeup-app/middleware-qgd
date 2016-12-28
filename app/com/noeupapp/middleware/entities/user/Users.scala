package com.noeupapp.middleware.entities.user

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator


import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope

import com.noeupapp.middleware.entities.account.Account
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import User._

import scalaz.{-\/, \/-}

/**
 * The user controller
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 */
class Users @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, BearerTokenAuthenticator],
                         scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                         userService: UserService)
  extends Silhouette[Account, BearerTokenAuthenticator] {


  def me = SecuredAction.async { implicit request =>
    userService.findById(request.identity.user.id) map {
      case \/-(None) => NotFound("Cannot to fetch your data, not found")
      case \/-(user) => Ok(Json.toJson(user))
      case -\/(e) =>  InternalServerError("Error while creating process" + e)
    }
  }

  def add(orGet: Option[String]) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.Processes"*/), WithRole()))
    .async(parse.json[UserIn]) { implicit request =>
      val newUser = request.request.body
      userService.add(newUser, orGet) map {
        case \/-(createdUser)  => Ok(Json.toJson(createdUser))
        case -\/(_)           => InternalServerError("Error while creating process")
      }
  }

  /**
    * Find user by ID
    *
    * @return
    */
  def fetchById(id: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      // TODO limit search to users I can admin
      userService.findById(id) map {
        case -\/(_) => InternalServerError(Json.toJson("Error while fetching users"))
        case \/-(Some(user)) => Ok(Json.toJson(toUserOut(user)))
        case \/-(None) => NoContent
      }
    }

  /**
    * Find all users
    *
    * @return
    */
  def fetchAll(email: Option[String]) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      // TODO limit search to users I can admin
      userService.findAll(email) map {
        case -\/(_) => InternalServerError(Json.toJson("Error while fetching users"))
        case \/-(usersList) => Ok(Json.toJson(usersList.map(u => toUserOut(u))))
      }
    }

}
