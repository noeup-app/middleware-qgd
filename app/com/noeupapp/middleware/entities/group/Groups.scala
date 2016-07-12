package com.noeupapp.middleware.entities.group

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.group.Group._
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json

import scalaz._

import scala.concurrent.ExecutionContext.Implicits.global



class Groups @Inject()(
                       val messagesApi: MessagesApi,
                       val env: Environment[Account, BearerTokenAuthenticator],
                       scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                       groupService: GroupService)
  extends Silhouette[Account, BearerTokenAuthenticator] {

  def fetchById(groupId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async { implicit request =>
      // TODO limit search to users I can admin
      val user = request.identity.user.id
      groupService.findById(groupId, user) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching group"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  def fetchAll = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async { implicit request =>
      // TODO limit search to users I can admin
      val user = request.identity.user.id
      groupService.findAll(user) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching groups"))
        case \/-(groups) => Ok(Json.toJson(groups))
      }
    }

  def addGroup() = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async(parse.json[GroupIn]) { implicit request =>
      // TODO limit search to users I can admin
      val groupIn = request.request.body
      val user = request.identity.user.id
      groupService.addGroupCheck(user, groupIn) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while creating groups"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  def deleteGroup(groupId: UUID) = SecuredAction(WithScope(/*"builder.groups"*/))
    .async { implicit request =>
      val user = request.identity.user.id
      groupService.deleteGroup(groupId, user) map {
        case -\/(error)          =>
          Logger.error(error.toString)
          InternalServerError("Error while deleting call")
        case \/-(None)       => NotFound("Error while deleting call, call not found")
        case \/-(Some(group)) => Ok(Json.toJson(group))
      }
    }

}
