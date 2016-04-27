package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject
import com.noeupapp.middleware.entities.user.{UserIn, User}

import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaz.{-\/, EitherT, OptionT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._


class UserService @Inject()(userDAO: UserDAO,
                            passwordInfoDAO: PasswordInfoDAO,
                            passwordHasher: PasswordHasher) {

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


  /**
    * Add new user
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
    * Validate a user thanks to it email and password
    * @param email user email
    * @param password non hashed pwd
    */
  def validateUser(email: String, password: String): Future[Option[User]] = {
    for{
      user         <- OptionT(findByEmail(email))
      passwordInfo <- OptionT(passwordInfoDAO.find(LoginInfo("credentials", email)))
    } yield {
      passwordHasher.matches(passwordInfo, password) match {
        case true => Some(user)
        case false => None
      }
    }
  }.run.map(_.flatten)

}
