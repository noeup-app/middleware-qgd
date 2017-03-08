package com.noeupapp.middleware.entities.account

import java.util.{NoSuchElementException, UUID}
import javax.inject.Inject

import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.authorizationClient.loginInfo.{AuthLoginInfo, AuthLoginInfoService}
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.entities.user.{User, UserIn, UserOut, UserService}
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.oauth2.TierAccessTokenConfig
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import play.api.mvc.Results.BadRequest


/**
  * Handles actions to users.
  */
class AccountService @Inject()(userService: UserService,
                               roleService: RoleService,
                               organisationService: OrganisationService,
                               authLoginInfoService: AuthLoginInfoService,
                               tierAccessTokenConfig: TierAccessTokenConfig)
  extends IdentityService[Account] {

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  override def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = {
    {
        for {
          authLoginInfoOpt <- EitherT(authLoginInfoService.find(loginInfo))
          authLoginInfo    <- EitherT(authLoginInfoOpt |> "authLoginInfoOpt is not defined")
          userOpt          <- EitherT(userService.findById(authLoginInfo.user))
          user             <- EitherT(userOpt |> (s"User not found with the following UUID:<${authLoginInfo.user}>", BadRequest))
          organisation     <- EitherT(userService.findOrganisationByUserId(user.id))
          userRoles        <- EitherT(roleService.getRolesByUser(user.id))
        } yield Account(loginInfo, user, userRoles, organisation)
      }.run map {
        case -\/(e) =>
          Logger.error(s"User not found $e")
          None
        case \/-(res) => Some(res)
      }
  }.recover{
         case e: NoSuchElementException =>
           Logger.info(s"User ${loginInfo.providerKey} not found $e")
           None
      }


  def retrieveWithRoles(email: Option[String], userId: UUID): Future[Expect[Option[(Account, List[String])]]] = {
    val loginInfo = api.LoginInfo("credentials", email.getOrElse(""))
    for{
      accountOpt <- EitherT(retrieve(loginInfo).map[Expect[Option[Account]]](\/-(_)))
      roles      <- EitherT(roleService.getRolesByUser(userId))
    } yield accountOpt.map((_, roles))
  }.run



  def createOrRetrieve(profile: CommonSocialProfile): Future[Account] = {
    profile.email match {
      case None => Future.failed(new Exception("Email is not defined !"))
      case Some(email) =>

        val userIn = UserIn(
          firstName = profile.firstName,
          lastName = profile.lastName,
          email = profile.email,
          avatarUrl = profile.avatarURL,
          ownedByClient = None
        )

        val account = Account(
          loginInfo = profile.loginInfo,
          user = userIn.toUser,
          List(),
          None
        )

        {
          for {
            userOpt          <- EitherT(userService.findByEmail(email))
            authLoginInfoOpt <- EitherT(authLoginInfoService.find(profile.loginInfo))
            accountRes       <- EitherT(handleUserOptAndAuthLoginInfoOpt(userOpt, authLoginInfoOpt, account, profile.loginInfo))
          } yield accountRes

        }.run map {
          case -\/(error) => throw new Exception(s"Error occurred while trying to get user by email : email = $email ; error = $error !")
          case \/-(accountRes) => accountRes
        }
    }
  }


  private def handleUserOptAndAuthLoginInfoOpt(userOpt: Option[User], authLoginInfoOpt: Option[AuthLoginInfo], account: Account, loginInfo: LoginInfo): Future[Expect[Account]] = {

    (userOpt, authLoginInfoOpt) match {

      case (Some(user), Some(authLoginInfo)) if user.id == authLoginInfo.user =>
        Future.successful(\/-(account.copy(user = user)))

      case (Some(user), Some(authLoginInfo)) => // Problem ! you are trying to connect with some provider that is already linked to another user. May append when user has changed his/her email in the provider
        Future.failed(new Exception("TODO"))

      case (Some(user), None) => // May append when you try to link a new provider ton an existing account
        authLoginInfoService.add(AuthLoginInfo.fromLoginInfo(loginInfo, user.id)).map(_.map(_ => account))

      // Never append because authLoginInfo has a foreign key linked to a user : a authLoginInfo can not exist without user
      // case (None, Some(authLoginInfo)) =>

      case (None, None) =>
        save(account)

    }
  }

  def save(account: Account): Future[Expect[Account]] =
    save(account.loginInfo, account.user, account.organisation)

  def save(loginInfo: LoginInfo, userInput: User, organisation: Option[Organisation] = None): Future[Expect[Account]] = {
    for {
      user      <- EitherT(userService.add(userInput))
      _         <- EitherT(authLoginInfoService.add(AuthLoginInfo.fromLoginInfo(loginInfo, user.id)))
      userRoles <- EitherT(roleService.getRolesByUser(user.id))
    } yield Account(loginInfo, user, userRoles, organisation)
  }.run

}
