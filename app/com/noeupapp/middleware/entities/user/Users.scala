package com.noeupapp.middleware.entities.user

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.entity.EntityService
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import User._
import play.api.Logger

import scalaz.{-\/, \/-}

/**
 * The user controller
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 */
class Users @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, CookieBearerTokenAuthenticator],
                         scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                         userService: UserService,
                         entityService: EntityService )
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {


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
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching users"))
        case \/-(usersList) =>
          Ok(Json.toJson(usersList))
      }
    }


  def update(id: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async(parse.json[User]) { implicit request =>
      userService.update(id, request.body) map {
        case \/-(_) => Ok("User updated")
        case -\/(e) =>
          Logger.error(e.toString)
          InternalServerError(Json.toJson("Error while updating user"))
      }
    }

  def delete(email: String, purge: Option[Boolean], cascade: Option[Boolean]) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      userService.delete(email, purge.getOrElse(false), cascade.getOrElse(false)) map {
        case \/-(true) => Ok("User deleted")
        case \/-(false) => InternalServerError("User not deleted")
        case -\/(e) =>
          Logger.error(e.toString)
          InternalServerError(Json.toJson("Error while deleting user"))
      }
    }

  def deleteUserParent(userId: UUID, parentId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      entityService.removeHierarchy(userId, Some(parentId)) map {
        case \/-(true) => Ok("parent deleted")
        case \/-(false) => InternalServerError("parent not deleted")
        case -\/(e) =>
          Logger.error(e.toString)
          InternalServerError(Json.toJson("Error while deleting parent"))
      }
    }

  def deleteAllUserParents(userId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      entityService.removeHierarchy(userId, None) map {
        case \/-(true) => Ok("all parents deleted")
        case \/-(false) => InternalServerError("parents not deleted")
        case -\/(e) =>
          Logger.error(e.toString)
          InternalServerError(Json.toJson("Error while deleting parents"))
      }
    }

  def getAllUserParents(userId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*/*"builder.steps"*/*/), WithRole("admin")))
    .async { implicit request =>
      entityService.getHierarchy(userId) map {
          case -\/(error) =>
            Logger.error(error.toString)
            InternalServerError(Json.toJson("Error while fetching groups for user"))
          case \/-(groupsList) =>
            Ok(Json.toJson(groupsList))
        }
    }
}
