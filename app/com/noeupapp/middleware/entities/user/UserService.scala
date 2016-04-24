package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO
import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaz.{-\/, EitherT, OptionT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.errorHandle.ExceptionEither._


class UserService @Inject()(userDAO: UserDAO,
                            passwordInfoDAO: PasswordInfoDAO,
                            passwordHasher: PasswordHasher,
                            entityService: EntityService,
                            organisationService: OrganisationService) {

  // TODO merge findByEmail and findByEmailEither ?
  def findByEmail(email: String): Future[Option[User]] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.find(email)
        })
      } catch {
        case e: Exception => Logger.error(s"UserService.findByEmail($email)", e)
          None
      }
    }
  }

//  def findByEmailEither(email: String): Future[Expect[User]] = {
//    TryBDCall{ implicit c =>
//      userDAO.find(email) match {
//        case Some(user) => \/-(user)
//        case None       => -\/(FailError("User not found"))
//      }
//    }
//  }

  def findByEmailEither(email: String): Future[Expect[User]] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.find(email) match {
            case Some(user) => \/-(user)
            case None => -\/(FailError("Cannot find user"))
          }
        })
      } catch {
        case e: Exception =>
          -\/(FailError("Error whole finding user", e))
      }
    }
  }


  def findOrganisationByUserId(userId: UUID): Future[Expect[Option[Organisation]]] = {
    {
      for{
        entity       <- EitherT(entityService.findById(userId))
        organisation <- EitherT(organisationService.fetchOrganisation(entity.parent.get))
      } yield organisation
    }.run
  }.recover{
    // If the entity.parent.get failed, it mean that entity.parent is None
    case e: NoSuchElementException => \/-(None)
  }

  def findById(id: UUID): Future[Option[User]] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.find(id)
        })
      } catch {
        case e: Exception =>
          Logger.error(s"UserService.findById($id)", e)
          None
      }
    }
  }

  def add(user: User): Future[Boolean] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.add(user)
        })
      } catch {
        case e: Exception =>
          Logger.error(s"UserService.add($user)", e)
          false
      }
    }
  }

  /**
    * Validate a user thanks to it email and password
    * @param email user email
    * @param password non hashed pwd
    */
  def validateUser(email: String, password: String): Future[Option[User]] = {
    Logger.debug(s"UserService.validateUser($email, ******)...")
    for{
      user         <- OptionT(findByEmail(email))
      passwordInfo <- OptionT(passwordInfoDAO.find(LoginInfo("credentials", email)))
    } yield {
      passwordHasher.matches(passwordInfo, password) match {
        case true =>
          Logger.debug(s"UserService.validateUser($email, ******) -> Some($user)")
          Some(user)
        case false =>
          Logger.debug(s"UserService.validateUser($email, ******) -> None")
          None
      }
    }
  }.run.map(_.flatten)

}
