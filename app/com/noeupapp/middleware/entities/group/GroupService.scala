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

  def findByIdFlow(groupId: UUID, userId: UUID): Future[Expect[Option[Group]]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      group <- EitherT(findById(groupId, userId, admin))
    } yield group
  }.run

  def findById(groupId: UUID, userId: UUID, admin: Boolean): Future[Expect[Option[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getById(groupId, userId, admin))
    }
  }

  def findAllFlow(userId: UUID): Future[Expect[List[Group]]] = {
    for {

      admin <- EitherT(isAdmin(userId))

      groups <- EitherT(findAll(userId, admin))
    } yield groups
  }.run

  def findAll(userId: UUID, admin: Boolean): Future[Expect[List[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getAll(userId, admin))
    }
  }

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

  def addGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      groupDAO.add(group)
      \/-(group)
    }
  }

  def findMembersFlow(groupId: UUID, userId: UUID): Future[Expect[List[GroupMember]]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      findGroup <- EitherT(findById(groupId, userId, admin))

      group <- EitherT(findGroup |> "Couldn't find this group")

      members <- EitherT(findMembers(groupId))
    } yield members
  }.run

  def findMembers(groupId: UUID): Future[Expect[List[GroupMember]]] = {
    TryBDCall { implicit c =>
      val members = groupDAO.findMembers(groupId)
      \/-(members)
    }
  }

  def addEntitiesFlow(groupId: UUID, userId: UUID, entities: Array[UUID]): Future[Expect[Group]] = {
    for {
      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to add members to group")

      findGroup <- EitherT(findById(groupId, userId, admin))

      group <- EitherT(findGroup |> "Couldn't find this group")

      members <- EitherT(addEntities(groupId, entities))

    } yield group
  }.run

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

  def addHierarchy(groupId: UUID, entityId: UUID): Future[Expect[UUID]] = {
    TryBDCall { implicit c =>
      groupDAO.addHierarchy(groupId, entityId)
      \/-(entityId)
    }
  }

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

  def updateGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      groupDAO.update(group)
      \/-(group)
    }
  }

  def isAdmin(userId: UUID): Future[Expect[Boolean]] = {
    TryBDCall { implicit c =>
      groupDAO.findAdmin.filter(entity => entity.id.equals(userId)) match {
        case Nil => \/-(false)
        case entity => \/-(true)
      }
    }
  }

  def deleteGroupFlow(groupId: UUID, userId: UUID): Future[Expect[Group]] = {
    for {

      admin <- EitherT(isAdmin(userId))

      validUser <- EitherT(admin |> "You are not authorized to delete groups")

      findGroup <- EitherT(findById(groupId, userId, admin))

      groupToDelete <- EitherT(findGroup |> "Couldn't find this group")

      group <- EitherT(deleteGroup(groupToDelete))

    } yield group
  }.run

  def deleteGroup(group: Group): Future[Expect[Group]] = {
    TryBDCall { implicit c =>

        groupDAO.delete(group.id)
        \/-(group.copy(deleted = true))

    }
  }
}
