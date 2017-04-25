package com.noeupapp.middleware.entities.group

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.entities.entity.{EntityOut, EntityService}
import com.noeupapp.middleware.entities.organisation.Organisation
import com.noeupapp.middleware.errorHandle.FailError

import scalaz._
import play.api.Logger

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
  def findByIdFlow(groupId: UUID, userId: UUID, organisation: Option[Organisation]): Future[Expect[Option[Group]]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      admin <- EitherT(isAdmin(userId, org.id))

      group <- EitherT(findById(groupId, userId, admin, org.id))
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
  def findById(groupId: UUID, userId: UUID, admin: Boolean, organisation: UUID): Future[Expect[Option[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getById(groupId, userId, admin, organisation))
    }
  }

  /**
    * Check if user is admin and call findAll
    *
    * @param userId
    * @return
    */
  def findAllFlow(userId: UUID, organisation: Option[Organisation]): Future[Expect[List[Group]]] = {
    for {

      //org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      //admin <- EitherT(isAdmin(userId, org.id))

      groups <- EitherT(findAll(userId/*, admin, org.id*/))
    } yield groups
  }.run

  /**
    * Find all groups user can see
    *
    * @param userId
    * @param admin
    * @return
    */
  def findAll(userId: UUID/*, admin: Boolean, organisation: UUID*/): Future[Expect[List[Group]]] = {
    TryBDCall { implicit c =>
      //Logger.debug(admin.toString)
      \/-(groupDAO.getAll(userId/*, admin, organisation*/))
    }
  }

  /**
    * Verify if user is admin and call addGroup
    *
    * @param userId
    * @param groupIn
    * @return
    */
  def addGroupCheck(userId: UUID, groupIn: GroupIn, organisation: Option[Organisation]): Future[Expect[Group]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      //admin <- EitherT(isAdmin(userId, org.id))

      //validUser <- EitherT(admin |> "You are not authorized to add groups")
      group <- EitherT(addGroup(Group(UUID.randomUUID(),
                                      groupIn.name,
                                      userId,
                                      deleted = false
                                      )))
      hierarchy <- EitherT(entityService.addHierarchy(org.id, group.id))
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
  def findMembersFlow(groupId: UUID, userId: UUID, organisation: Option[Organisation]): Future[Expect[GroupMembers]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      admin <- EitherT(isAdmin(userId, org.id))

      findGroup <- EitherT(findById(groupId, userId, admin, org.id))

      group <- EitherT(findGroup |> "Couldn't find this group")

      members <- EitherT(findMembers(groupId, org.id))
    } yield GroupMembers(group.name, members)
  }.run

  /**
    * Find group members
    *
    * @param groupId
    * @return
    */
  def findMembers(groupId: UUID, organisation: UUID): Future[Expect[List[EntityOut]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.findMembers(groupId, organisation))
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
  def addEntitiesFlow(groupId: UUID, userId: UUID, entities: Array[UUID], organisation: Option[Organisation]): Future[Expect[Group]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      admin <- EitherT(isAdmin(userId, org.id))

      validUser <- EitherT(admin |> "You are not authorized to add members to group")

      findGroup <- EitherT(findById(groupId, userId, admin, org.id))

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
        entityService.addHierarchy(groupId, entity)
      }
    } match {
      case -\/(_) => Future.successful(-\/(FailError("Error while adding entity")))
      case \/-(_) => Future.successful(\/-(entities))
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
  def updateGroupFlow(groupId: UUID, userId: UUID, groupUpdate: GroupIn, organisation: Option[Organisation]): Future[Expect[Group]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      admin <- EitherT(isAdmin(userId, org.id))

      validUser <- EitherT(admin |> "You are not authorized to update groups")

      findGroup <- EitherT(findById(groupId, userId, admin, org.id))

      groupToUpdate <- EitherT(findGroup |> "Couldn't find this group")

      group <- EitherT(updateGroup(Group(groupId,
                                         groupUpdate.name,
                                         groupUpdate.owner.getOrElse(groupToUpdate.owner),
                                         deleted = false
                                         ), org.id))
    } yield group
  }.run

  /**
    * Update group's name or owner
    *
    * @param group
    * @return
    */
  def updateGroup(group: Group, organisation: UUID): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      groupDAO.update(group, organisation)
      \/-(group)
    }
  }

  /**
    * Check if user is part of admins
    *
    * @param userId
    * @return
    */
  def isAdmin(userId: UUID, organisation: UUID): Future[Expect[Boolean]] = {
    TryBDCall { implicit c =>
      groupDAO.findAdmin(organisation).filter(entity => entity.id.equals(userId)) match {
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
  def deleteGroupFlow(groupId: UUID, userId: UUID, organisation: Option[Organisation], force_delete:Option[Boolean]): Future[Expect[UUID]] = {
    for {
      org <- EitherT(organisation |> "You need to be part of an organisation to access groups")
      //admin <- EitherT(isAdmin(userId, org.id))

      //validUser <- EitherT(admin |> "You are not authorized to delete groups")

      //findGroup <- EitherT(findById(groupId, userId, admin, org.id))

      //groupToDelete <- EitherT(findGroup |> "Couldn't find this group")
      //adminGroup <- EitherT(!groupToDelete.name.equals("Admin") |> "You can't delete an admin group")

      group <- EitherT(deleteGroup(groupId, org.id, force_delete:Option[Boolean]))

    } yield group
  }.run

  /**
    * Delete a group
    *
    * @param group
    * @return
    */
  def deleteGroup(group: UUID, organisation: UUID, force_delete:Option[Boolean]): Future[Expect[UUID]] = {
    TryBDCall { implicit c =>
        groupDAO.delete(group, organisation, force_delete.getOrElse(false))
        \/-(group)
    }
  }
}
