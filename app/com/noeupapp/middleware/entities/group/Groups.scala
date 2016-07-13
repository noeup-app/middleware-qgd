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

  /**
    * Fetch group information knowing its ID
    *
    * @param groupId
    * @return
    */
  def fetchById(groupId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async { implicit request =>
      val user = request.identity.user.id
      groupService.findByIdFlow(groupId, user) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching group"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  /**
    * Fetch all groups user can see
    *
    * @return
    */
  def fetchAll = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async { implicit request =>
      val user = request.identity.user.id
      groupService.findAllFlow(user) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching groups"))
        case \/-(groups) => Ok(Json.toJson(groups))
      }
    }

  /**
    * Fetch members of a group
    *
    * @param groupId
    * @return
    */
  def fetchMembers(groupId: UUID)= SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async { implicit request =>
      val user = request.identity.user.id
      groupService.findMembersFlow(groupId, user) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching groups"))
        case \/-(members) => Ok(Json.toJson(members))
      }
    }

  /**
    * Add a new group
    * Admin only
    *
    * @return
    */
  def addGroup() = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async(parse.json[GroupIn]) { implicit request =>
      val groupIn = request.request.body
      val user = request.identity.user.id
      groupService.addGroupCheck(user, groupIn) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while creating group"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  /**
    * Add new members to a group
    * Admin only
    *
    * @param groupId
    * @return
    */
  def addEntities(groupId: UUID)= SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async(parse.json[Array[UUID]]) { implicit request =>
      val entities = request.request.body
      val user = request.identity.user.id
      groupService.addEntitiesFlow(groupId, user, entities) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while creating group"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  /**
    * Update a group's name or owner
    * Admin only
    *
    * @param groupId
    * @return
    */
  def updateGroup(groupId: UUID) = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.groups*/), WithRole("admin")))
    .async(parse.json[GroupUpdate]) { implicit request =>
      val groupUp = request.request.body
      val user = request.identity.user.id
      groupService.updateGroupFlow(groupId, user, groupUp) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while updating group"))
        case \/-(group) => Ok(Json.toJson(group))
      }
    }

  /**
    * Delete a group
    * Admin only
    *
    * @param groupId
    * @return
    */
  def deleteGroup(groupId: UUID) = SecuredAction(WithScope(/*"builder.groups"*/))
    .async { implicit request =>
      val user = request.identity.user.id
      groupService.deleteGroupFlow(groupId, user) map {
        case -\/(error)          =>
          Logger.error(error.toString)
          InternalServerError("Error while deleting call")
        case \/-(group) => Ok(Json.toJson(group))
      }
    }
}
