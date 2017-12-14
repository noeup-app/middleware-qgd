package com.noeupapp.middleware.entities.user

import java.util.UUID
import javax.inject.Inject

import play.api.mvc.Results._
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.noeupapp.middleware.authorizationClient.authInfo.PasswordInfoDAO
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmailService
import com.noeupapp.middleware.authorizationClient.customAuthenticator.{CookieBearerTokenAuthenticator, CookieBearerTokenAuthenticatorDAO}
import com.noeupapp.middleware.authorizationClient.loginInfo.AuthLoginInfoService
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.entity.EntityService
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
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
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.oauth2.TierAccessTokenConfig
import com.noeupapp.middleware.utils.TypeConversion
import play.api.libs.json.Json


class UserService @Inject()(userDAO: UserDAO,
                            passwordInfoDAO: PasswordInfoDAO,
                            passwordHasher: PasswordHasher,
                            entityService: EntityService,
                            organisationService: OrganisationService,
                            tierAccessTokenConfig: TierAccessTokenConfig,
                            authLoginInfoService: AuthLoginInfoService,
                            cookieBearerTokenAuthenticatorDAOProvider: Provider[CookieBearerTokenAuthenticatorDAO],
                            accountServiceProvider: Provider[AccountService]//,
                            //_accountService : AccountService
                            ) {

  private lazy val accountService = accountServiceProvider.get()
  private lazy val cookieBearerTokenAuthenticatorDAO = cookieBearerTokenAuthenticatorDAOProvider.get()

  type ValidationFuture[A] = EitherT[Future, FailError, A]

  /**
    * Search and get all users
    *
    * @return List of users
    */
  def findAll(email:Option[String]): Future[Expect[List[User]]] = {
    TryBDCall[List[User]]{ implicit c =>
      \/-(userDAO.findAll(email))
    }
  }

  def findByEmail(email: String, clientId: Option[String] = None): Future[Expect[Option[User]]] = {
    clientId match {
      case Some(client) =>
        TryBDCall{ implicit c =>
          \/- (userDAO.find(email, client))
        }
      case None =>
        TryBDCall{ implicit c => {
            val res = userDAO.find(email)
            \/- (res)
          }
        }
    }
  }


  def findDeletedOrNotByEmail(email: String): Future[Expect[Option[User]]] =
    TryBDCall{ implicit c =>
      val res = userDAO.findDeletedOrNot(email)
      \/- (res)
    }

  def findInactive(email: String): Future[Expect[Option[User]]] = {
    TryBDCall{ implicit c =>
      \/- (userDAO.findInactive(email))
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
    * Get email from user Option
    * @param user Option[User]
    * @return
    */
  def getEmailFromUser(user: Option[User]): Future[Expect[String]] = {
    user match {
      case Some(user) =>
        user.email match {
          case Some(email) =>  Future.successful(\/-(email))
          case None => Future(-\/(FailError("User email not set")))
        }
      case None => Future(-\/(FailError("User doesn't exist")))
    }
  }


  /**
    * Get User from Option[User]
    * @param user Option[User] to check
    * @return
    */
  def getUserFromOpt(user: Option[User]): Future[Expect[User]] = {
    user match {
      case Some(user) => Future.successful(\/-(user))
      case None => Future(-\/(FailError("User doesn't exist")))
    }
  }


  def findUserByToken(token: String): Future[Expect[Option[User]]] = {
    for{
      cookieBearerAuthOpt <- EitherT(tryFutures(cookieBearerTokenAuthenticatorDAO.find(token)))
      cookieBearerAuth    <- EitherT(cookieBearerAuthOpt |> "Token is not found")
      _                   <- EitherT(cookieBearerAuth.isValid |> "Token is not valid anymore")
      accountOpt          <- EitherT(tryFutures(accountService.retrieve(cookieBearerAuth.loginInfo)))
    } yield accountOpt.map(_.user)
  }.run


  /**
    * Add new user
    *
    * @param userInput
    * @param orGet
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
              //{
              //  var loginInfo = LoginInfo(CredentialsProvider.ID, userInput.email.get)
              //  _accountService.save(loginInfo, userInput.toUser).map {
              //    case \/-(u) => \/-(u.user.toUserOut)
              //    case -\/(error) => -\/(error)
              //  }
              //}
            }
        }
      case None => addDao(userInput)
      //{
      //  var loginInfo = LoginInfo(CredentialsProvider.ID, userInput.email.get)
      //  _accountService.save(loginInfo, userInput.toUser).map {
      //    case \/-(u) => \/-(u.user.toUserOut)
      //    case -\/(error) => -\/(error)
      //  }
      //}
    }
  }

  private def addDao(userInput: UserIn): Future[Expect[UserOut]] = {
    TryBDCall[UserOut] { implicit c =>
      val user = userInput.toUser
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
                        false,
                        Some(tierAccessTokenConfig.tierClientId)
      )
      userDAO.add(user)
      user
    })
  }

  def add(user: User): Future[Expect[User]] =
    TryBDCall { implicit c =>
      userDAO.add(user)
      \/-(user)
    }


  /**
    * Add user with active status to false
    * @param userInput User to add
    * @return
    */
  def addInactive(userInput: UserIn): Future[User] = Future {
    DB.withTransaction({ implicit c =>
      val userId = UUID.randomUUID()
      val user = User(userId,
                      userInput.firstName,
                      userInput.lastName,
                      userInput.email,
                      userInput.avatarUrl,
                      DateTime.now,
                      active = false,
                      deleted = false,
                      userInput.ownedByClient
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
    Logger.debug(s"--- Into validateUser --- Email: $email")
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

  /**
    * Update user active status
    * @param userId userId
    * @param status true or false
    * @return
    */
  def changeActiveStatus(userId: UUID, status :Boolean): Future[Expect[Boolean]] = {
    TryBDCall{ implicit c =>
      \/-(userDAO.updateActive(userId, status))
    }
  }
  /**
    * Get number of active user in db
    * @return
    */
  def getNumberActiveUser(): Future[Expect[Int]] = {
    TryBDCall{ implicit c =>
      \/-(userDAO.countActiveUsers())
    }
  }



  def update(id: UUID, body: User): Future[Expect[Unit]] = {
    TryBDCall{ implicit c =>
      \/-(userDAO.update(id, body))
    }
  }



  /**
    * Set deleted field to true
    * @param userId user to delete
    * @return
    */
  def deleteUserById(userId: UUID): Future[Expect[Boolean]] = {
    TryBDCall{ implicit c =>
      \/-(userDAO.delete(userId))
    }
  }

  def deletePurgeUserById(userId: UUID): Future[Expect[Boolean]] = {
    TryBDCall{ implicit c =>
      \/-(userDAO.deletePurge(userId))
    }
  }

  /**
    * Update contact's deleted field to true
    * @param email
    * @return
    */
  def delete(email: String, purge: Boolean, cascade: Boolean): Future[Expect[Boolean]] = {

    def getUser: EitherT[Future, FailError, Option[User]] =
      if(purge)
        EitherT(this.findDeletedOrNotByEmail(email))
      else
        EitherT(this.findByEmail(email))

    def deleteCascade(userId: UUID) =
      EitherT({
        for {
          _ <- EitherT(authLoginInfoService.delete(userId))
          _ <- EitherT(this.deletePurgeUserById(userId))
        } yield true
      }.run)

    def deleteUser(userId: UUID): EitherT[Future, FailError, Boolean] =
      if(purge)
        if(cascade) deleteCascade(userId)
        else EitherT(this.deletePurgeUserById(userId))
      else
        EitherT(this.deleteUserById(userId))

    for{
      userOpt <- getUser
      user    <- EitherT(userOpt |> s"User ($email) is not defined")
      _       <- deleteUser(user.id)
    } yield true
  }.run

}
