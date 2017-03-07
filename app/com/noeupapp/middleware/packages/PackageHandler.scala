package com.noeupapp.middleware.packages

import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.relationEntityPackage.{RelationEntityPackage, RelationEntityPackageService}
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.packages.pack.Pack
import com.noeupapp.middleware.utils.FutureFunctor._

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}

trait PackageHandler {

  val actionPackage: ActionPackage
  val relationEntityPackageService: RelationEntityPackageService

  /**
    * Check if the user have access to actionName
    * @param user
    * @param actionName
    * @return
    */
  def isAuthorized(user: User, actionName: String): Future[Expect[Unit]] = {
    for {
      packs      <- EitherT(getPacks(actionName))
      relEntPack <- EitherT(hasAccessPackage(packs, user))
      _          <- EitherT(jsonProcess(actionName, user, packs, relEntPack))
    } yield ()
  }.run


  protected def jsonProcess(actionName: String, user: User, usersPackages: Set[Pack], relEntPack: RelationEntityPackage): Future[Expect[Unit]]

  private def getPacks(actionName: String): Future[Expect[Set[Pack]]] =
    Future.successful(\/-(actionPackage.packages(actionName)))

  private def hasAccessPackage(packs: Set[Pack], user: User): Future[Expect[RelationEntityPackage]] = {
    val userInfo = s"User ${user.firstName} ${user.lastName} <${user.email}>"

    def checkPack(packageId: Option[Long]): Future[Expect[Unit]] = {
      val packsDescription = packs.map(pack => s"${pack.name}[${pack.id}]").mkString(", ")

      packageId.map(packs.map(_.id).contains) match {
        case Some(true)   => Future.successful(\/-(s"User $userInfo can access to $packsDescription"))
        case Some(false)  =>
          Future.successful(-\/(FailError(s"$userInfo doesn't have access this(those) pack(s): $packsDescription")))
        case None => Future.successful(-\/(FailError(s"$userInfo doesn't have a pack")))
      }
    }

    for {
      packOpt <- EitherT(relationEntityPackageService.getUsersActivePackage(user.id))
      _       <- EitherT(checkPack(packOpt.map(_.packageId)))
    } yield packOpt.get // safe thanks to `checkPack`
  }.run

}
