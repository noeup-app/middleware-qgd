package com.noeupapp.middleware.entities.group

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.group.Group._
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeConversion
import com.noeupapp.middleware.utils.TypeCustom._

import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.errorHandle.FailError

import scalaz._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GroupService @Inject()(groupDAO: GroupDAO,
                             entityService: EntityService){

  /**
    * Check if user is admin and call findById
    *
    * @param groupId
    * @param userId
    * @return
    */
  def findByIdFlow(groupId: UUID, userId: UUID): Future[Expect[Option[Group]]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      group <- EitherT(findById(groupId, userId, admin))
    } yield group
  }.run

  /**
    * Find a group knowing its ID, if user can see it
    *
    * @param groupId
    * @param userId
    * @param admin
    * @return
    */
  def findById(groupId: UUID, userId: UUID, admin: Boolean): Future[Expect[Option[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getById(groupId, userId, admin))
    }
  }

  /**
    * Check if user is admin and call findAll
    *
    * @param userId
    * @return
    */
  def findAllFlow(userId: UUID): Future[Expect[List[Group]]] = {
    for {

      admin <- EitherT(isAdmin(userId))

      groups <- EitherT(findAll(userId, admin))
    } yield groups
  }.run

  /**
    * Find all groups user can see
    *
    * @param userId
    * @param admin
    * @return
    */
  def findAll(userId: UUID, admin: Boolean): Future[Expect[List[Group]]] = {
    TryBDCall { implicit c =>
      Logger.debug(admin.toString)
      \/-(groupDAO.getAll(userId, admin))
    }
  }

  /**
    * Verify if user is admin and call addGroup
    *
    * @param userId
    * @param groupIn
    * @return
    */
  def addGroupCheck(userId: UUID, groupIn: GroupIn): Future[Expect[Group]] = {
    for {

      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to add groups")

      group <- EitherT(addGroup(Group(UUID.randomUUID(),
                                      groupIn.name,
                                      userId,
                                      deleted = false
                                      )))
    } yield group
  }.run

  /**
    * Add a new group
    *
    * @param group
    * @return
    */
  def addGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      groupDAO.add(group)
      \/-(group)
    }
  }

  /**
    * Check if the group exists and call findMembers
    *
    * @param groupId
    * @param userId
    * @return
    */
  def findMembersFlow(groupId: UUID, userId: UUID): Future[Expect[List[GroupMember]]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      findGroup <- EitherT(findById(groupId, userId, admin))

      group <- EitherT(findGroup |> "Couldn't find this group")

      members <- EitherT(findMembers(groupId))
    } yield members
  }.run

  /**
    * Find group members
    *
    * @param groupId
    * @return
    */
  def findMembers(groupId: UUID): Future[Expect[List[GroupMember]]] = {
    TryBDCall { implicit c =>
      val members = groupDAO.findMembers(groupId)
      \/-(members)
    }
  }

  /**
    * Verify if user is admin and group exists
    * Call addEntities
    *
    * @param groupId
    * @param userId
    * @param entities
    * @return
    */
  def addEntitiesFlow(groupId: UUID, userId: UUID, entities: Array[UUID]): Future[Expect[Group]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to add members to group")

      findGroup <- EitherT(findById(groupId, userId, admin))

      group <- EitherT(findGroup |> "Couldn't find this group")

      members <- EitherT(addEntities(groupId, entities))

    } yield group
  }.run

  /**
    * For each entity, check if entity exists and call addHierarchy
    *
    * @param groupId
    * @param entities
    * @return
    */
  def addEntities(groupId: UUID, entities: Array[UUID]): Future[Expect[Array[UUID]]] = {

    Try {
      entities.foreach { entity =>
        entityService.findById(entity)
        addHierarchy(groupId, entity)
      }
    } match {
      case -\/(_) => Future.successful(-\/(FailError("Error while adding entity")))
      case \/-(_) => Future.successful(\/-(entities))
    }
  }

  /**
    * Creates a new hierarchy relation to link an entity to a parent group
    *
    * @param groupId
    * @param entityId
    * @return
    */
  def addHierarchy(groupId: UUID, entityId: UUID): Future[Expect[UUID]] = {
    TryBDCall { implicit c =>
      groupDAO.addHierarchy(groupId, entityId)
      \/-(entityId)
    }
  }

  /**
    * Verify if user is admin and group exists
    * Call updateGroup
    *
    * @param groupId
    * @param userId
    * @param groupUpdate
    * @return
    */
  def updateGroupFlow(groupId: UUID, userId: UUID, groupUpdate: GroupUpdate): Future[Expect[Group]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to update groups")

      findGroup <- EitherT(findById(groupId, userId, admin))

      groupToUpdate <- EitherT(findGroup |> "Couldn't find this group")

      group <- EitherT(updateGroup(Group(groupId,
                                         groupUpdate.name.getOrElse(groupToUpdate.name),
                                         groupUpdate.owner.getOrElse(groupToUpdate.owner),
                                         deleted = false
                                         )))
    } yield group
  }.run

  /**
    * Update group's name or owner
    *
    * @param group
    * @return
    */
  def updateGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      groupDAO.update(group)
      \/-(group)
    }
  }

  /**
    * Check if user is part of admins
    *
    * @param userId
    * @return
    */
  def isAdmin(userId: UUID): Future[Expect[Boolean]] = {
    TryBDCall { implicit c =>
      groupDAO.findAdmin.filter(entity => entity.id.equals(userId)) match {
        case Nil => \/-(false)
        case entity => \/-(true)
      }
    }
  }

  /**
    * Verify if user is admin and group exists
    * Call deleteGroup
    *
    * @param groupId
    * @param userId
    * @return
    */
  def deleteGroupFlow(groupId: UUID, userId: UUID): Future[Expect[Group]] = {
    for {

      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to delete groups")

      findGroup <- EitherT(findById(groupId, userId, admin))

      groupToDelete <- EitherT(findGroup |> "Couldn't find this group")

      group <- EitherT(deleteGroup(groupToDelete))

    } yield group
  }.run

  /**
    * Delete a group
    *
    * @param group
    * @return
    */
  def deleteGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>

        groupDAO.delete(group.id)
        \/-(group.copy(deleted = true))

    }
  }
}
