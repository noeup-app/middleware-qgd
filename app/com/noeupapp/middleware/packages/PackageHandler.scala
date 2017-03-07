package com.noeupapp.middleware.packages

import java.util.UUID

import com.noeupapp.middleware.crudauto.CrudAutoFactory
import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.relationEntityPackage.{RelationEntityPackage, RelationEntityPackageService}
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.packages.pack.Pack
import com.noeupapp.middleware.utils.FutureFunctor._
import play.api.{Configuration, Logger}
import play.api.libs.json._

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._



trait PackageHandler extends HttpMethods {

  val actionPackage: ActionPackage
  val relationEntityPackageService: RelationEntityPackageService
  val configuration: Configuration
  val packageService: CrudAutoFactory[Pack, Long]
  val relationEntityPackageCrudAutoService: CrudAutoFactory[RelationEntityPackage, UUID]

  private val PLAY_HTTP_CONTEXT = "play.http.context"

  private val httpContext = configuration.getString(PLAY_HTTP_CONTEXT).getOrElse("")
  private val numberOfCharToRemove = Math.max(0, httpContext.length - 1)


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


  def isAuthorized(user: User, httpMethod: String, withOutHttpContext: String): Future[Expect[Unit]] =
    isAuthorized(user, mapHttpToActionName(httpMethod, withOutHttpContext.substring(numberOfCharToRemove)))

  protected def mapHttpToActionName(httpMethod: String, withOutHttpContext: String): String

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

  type PackageState


  /**
    *
    * @param relEntPack
    * @param p predicate to check if the state of the package is ok
    * @param f update the package
    * @return
    */
  protected def mapPackageState(relEntPack: RelationEntityPackage)
                             (p: Option[PackageState] => Option[String])
                             (f: Option[PackageState] => Option[PackageState])
                             (implicit packageStateFormat: Format[PackageState]) = {
    for {
      pack <- EitherT(packageService.find(relEntPack.packageId))

      packOffer <- EitherT(getPackageStageAsJson(pack.flatMap(_.optionOffer)))
      packState <- EitherT(getPackageStageAsJson(relEntPack.optionState))

      usersPackOrDefault = packState match {
        case e @ Some(_) => e
        case None => packOffer
      }

      predicateRes = p(usersPackOrDefault)
      _ <- EitherT(predicateRes.isEmpty |> predicateRes.get)

      packStageUpdated = f(usersPackOrDefault)

      _ <- EitherT(relationEntityPackageCrudAutoService.update(
        relEntPack.id,
        relEntPack.copy(optionState = packStageUpdated.map(Json.toJson(_)))
      ))
    } yield ()
  }.run

  def getPackageStageAsJson(offer: Option[JsValue])(implicit packageStateFormat: Format[PackageState]): Future[Expect[Option[PackageState]]] =
    offer.map(_.validate[PackageState]) match {
      case Some(JsSuccess(value, _)) => Future.successful(\/-(Some(value)))
      case Some(JsError(errors)) =>
        Logger.error(s"Error while validating $errors")
        Future.successful(\/-(None))
      case None => Future.successful(\/-(None))
    }


}
