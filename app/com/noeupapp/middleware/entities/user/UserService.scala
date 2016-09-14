package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.user._
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.{PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO
import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import com.noeupapp.middleware.utils.TypeCustom._

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

  def findByEmail(email: String, clientId: Option[String] = None): Future[Expect[Option[User]]] = {
    clientId match {
      case Some(client) =>
        TryBDCall{ implicit c =>
          \/- (userDAO.find(email, client))
        }
      case None =>
        TryBDCall{ implicit c =>
          \/- (userDAO.find(email))
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

  /**
    * Add new user
    *
    * @param userInput
    * @return Complete user with UUID created
    */
  def add(userInput: UserIn, orGet: Option[String] = None): Future[Expect[UserOut]] = {
    orGet match {
      case Some(_) =>
        userInput.email match {
          case None        => Future.successful(-\/(FailError("Email is not defined")))
          case Some(email) =>
            findByEmail(email).flatMap{
              case \/-(Some(user)) => Future.successful(\/-(user))
              case e @ -\/(_) => Future.successful(e)
              case \/-(None)  => addDao(userInput)
            }
        }
      case None => addDao(userInput)
    }
  }

  private def addDao(userInput: UserIn): Future[Expect[UserOut]] = {
    TryBDCall[UserOut] { implicit c =>
      val userId = UUID.randomUUID()
      val user = User(
        id = userId,
        firstName = userInput.firstName,
        lastName = userInput.lastName,
        email = userInput.email,
        avatarUrl = userInput.avatarUrl,
        created = DateTime.now,
        active = true,
        deleted = false
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
                        DateTime.now,
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

    val result: ValidationFuture[Option[User]] =
    for{
      user         <- EitherT(findByEmail(email))
                      trace1 = Logger.trace(s"Login : Found existing user for $email")
      passwordInfo <- EitherT(passwordInfoDAO.find(LoginInfo("credentials", email)).map(TypeConversion.option2Expect))
    } yield {
      passwordHasher.matches(passwordInfo, password) match {
        case true =>
          Logger.debug(s"Login : $email successfully logged in ")
          user
        case false =>
          Logger.debug(s"Login : $email not logged in (password does not match")
          None
      }
    }
    result.run
  }


  private def changePasswordWithoutUserCheck(email: String, password: String): Future[Expect[Unit]] = {
    TryBDCall{ implicit c =>
      val loginInfo = LoginInfo("credentials", email)
      val passwordInfo = passwordHasher.hash(password)
      \/-(passwordInfoDAO.update(loginInfo, passwordInfo))
    }
  }


  def changePassword(email: String, password: String): Future[Expect[Unit]] = {
    for{
      user <- EitherT(this.findByEmail(email))
      _    <- EitherT(user |> "User is not defined")
      _    <- EitherT(this.changePasswordWithoutUserCheck(email, password))
    } yield ()
  }.run

}
