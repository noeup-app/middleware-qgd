package com.noeupapp.middleware.entities.group

import java.util.{NoSuchElementException, UUID}
import javax.inject.Inject

import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeConversion
import com.noeupapp.middleware.utils.TypeCustom._
import org.joda.time.DateTime
import java.sql.Connection

import scalaz._
import play.api.Logger


import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class GroupService @Inject()(groupDAO: GroupDAO){

  def findById(groupId: UUID, userId: UUID): Future[Expect[Option[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getById(groupId, userId))
    }
  }

  def findAll(userId: UUID): Future[Expect[List[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getAll(userId))
    }
  }

  def addGroup(userId: UUID, groupIn: GroupIn): Future[Expect[Group]] = {
    TryBDCall { implicit c =>
      val group = Group(UUID.randomUUID(),
                        groupIn.name,
                        userId,
                        deleted = false)
      groupDAO.add(group)
      \/-(group)
    }
  }

  def isAdmin(userId: UUID): Future[Expect[Boolean]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.isAdmin(userId))
      }
    }
  

  def deleteGroup(groupId: UUID, userId: UUID): Future[Expect[Option[Group]]] = {
    TryBDCall { implicit c =>
      \/-(groupDAO.getById(groupId, userId) map { group =>
        groupDAO.delete(groupId)
        group.copy(deleted = true)
      })
    }
  }
}
