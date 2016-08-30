package com.noeupapp.middleware.entities.entity

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.Future
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError

import scalaz.{-\/, \/-}

class EntityService @Inject() (entityDAO: EntityDAO) {

  def findById(entityId: UUID): Future[Expect[Entity]] = {
    TryBDCall{ implicit c =>
      entityDAO.findById(entityId) match {
        case Some(entity) => \/-(entity)
        case None => -\/(FailError("Entity not found"))
      }
    }
  }
  /**
    * Creates a new hierarchy relation to link an entity to a parent entity
    *
    * @param parentId
    * @param entityId
    * @return
    */
  def addHierarchy(parentId: UUID, entityId: UUID): Future[Expect[UUID]] = {
    TryBDCall { implicit c =>
      entityDAO.addHierarchy(parentId, entityId)
      \/-(entityId)
    }
  }
}
