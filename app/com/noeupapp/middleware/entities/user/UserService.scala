package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.user._
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO
import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.errorHandle.ExceptionEither._
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
import com.noeupapp.middleware.utils.TypeConversion


class UserService @Inject()(userDAO: UserDAO,
                            passwordInfoDAO: PasswordInfoDAO,
                            passwordHasher: PasswordHasher,
                            entityService: EntityService,
                            organisationService: OrganisationService) {

  type ValidationFuture[A] = EitherT[Future, FailError, A]

  /**
    * Search and get all users
    *
    * @return List of users
    */
  def findAll: Future[Expect[List[User]]] = {
    TryBDCall[List[User]]{ implicit c =>
      \/-(userDAO.findAll)
    }
  }

  // TODO merge findByEmail and findByEmailEither ?
  def findByEmail(email: String): Future[Expect[Option[User]]] = {
    TryBDCall{ implicit c =>
      \/- (userDAO.find(email))

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

  def findById(id: UUID): Future[Expect[Option[User]]] = {
    TryBDCall{ implicit c =>
      \/- (userDAO.find(id))

    }
  }


  def findLastName(user: User): Future[Expect[String]] = {
    user.lastName match {
      case Some(name) => Future.successful(\/-(name))
      case _ => Future.successful(-\/(FailError("User name not found")))
    }

  }

  def findFirstName(user: User): Future[Expect[String]] = {
    user.firstName match {
      case Some(name) => Future.successful(\/-(name))
      case _ => Future.successful(-\/(FailError("User name not found")))
    }

  }

  def findEmail(user: User): Future[Expect[String]] = {
    user.email match {
      case Some(email) => Future.successful(\/-(email))
      case _ => Future.successful(-\/(FailError("User email not found")))
    }

  }


  /**
    * Add new user
    *
    * @param userInput
    * @return Complete user with UUID created
    */
  def add(userInput: UserIn): Future[Expect[UserOut]] = {
    TryBDCall[UserOut]{ implicit c =>
      val userId = UUID.randomUUID()
      val user = User(  userId,
                        userInput.firstName,
                        userInput.lastName,
                        userInput.email,
                        userInput.avatarUrl,
                        true,
                        false
                      )
      userDAO.add(user)
      \/-(user)
    }
  }

  /**
    * QUICK FIX while using monad transformers in signUps
    * // TODO think about it
    */
  def simplyAdd(userInput: UserIn): Future[User] = Future {
    DB.withTransaction({ implicit c =>
      val userId = UUID.randomUUID()
      val user = User(  userId,
        userInput.firstName,
        userInput.lastName,
        userInput.email,
        userInput.avatarUrl,
        true,
        false
      )
      userDAO.add(user)
      user
    })
  }


  /**
    * Validate a user thanks to it email and password
    *
    * @param email user email
    * @param password non hashed pwd
    */
  def validateUser(email: String, password: String): Future[Expect[Option[User]]] = {
    Logger.debug(s"UserService.validateUser($email, ******)...")


    val result: ValidationFuture[Option[User]] =
    for{
      user         <- EitherT(findByEmail(email))
      passwordInfo <- EitherT(passwordInfoDAO.find(LoginInfo("credentials", email)).map(TypeConversion.option2Expect))
    } yield {
      passwordHasher.matches(passwordInfo, password) match {
        case true =>
          Logger.debug(s"UserService.validateUser($email, ******) -> Some($user)")
          user
        case false =>
          Logger.debug(s"UserService.validateUser($email, ******) -> None")
          None
      }
    }
    result.run
  }

}
