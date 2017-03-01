package com.noeupapp.middleware.packages

import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.packages.pack.Pack

import scala.concurrent.Future
import scalaz.{-\/, \/-}

trait PackageHandler {

  val actionPackage: ActionPackage
  val entityService: EntityService

  def isAuthorized(user: User, actionName: String): Future[Expect[Unit]] = {
    for {
      hasPackageAccess
    }
  }

  private def getPack(actionName: String): Future[Expect[Set[Pack]]] =
    Future.successful(\/-(actionPackage.packages(actionName)))

  private def hasAccessPackage(pack: Set[Pack], user: User): Future[Expect[Unit]] = {
    def checkPack(packageId: Option[Long]): Future[Expect[Unit]] = {
      packageId.map(pack.map(_.id).contains) match {
        case Some(true) => Future.successful(\/-())
        case Some(false) => Future.successful(-\/(FailError(s"")))
        case None =>
      }
    }

    for {
      packageId <- entityService.getPackageId(user.id)
      _ <- checkPack(packageId)
    } yield
  }

}
