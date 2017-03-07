package com.noeupapp.middleware.entities.relationEntityPackage

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.crudauto.Dao
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scalaz.{-\/, \/-}


/**
  * Created by damien on 06/03/2017.
  */
class RelationEntityPackageService @Inject()(dao: Dao){

  def getUsersActivePackage(userId: UUID): Future[Expect[Option[RelationEntityPackage]]] =
    dao.db
      .run(
        RelationEntityPackage.relEntPack
          .filter(_.entityId === userId)
          .filter(_.billed.isEmpty)
          .result
          .headOption
      )
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }

}
