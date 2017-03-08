package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmailService
import com.noeupapp.middleware.authorizationClient.forgotPassword.{ForgotPasswordConfig, ForgotPasswordService}
import com.noeupapp.middleware.authorizationClient.signUp.SignUpForm.Data
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.entities.user.{User, UserIn, UserService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.mailer.MessageEmail
import play.api.mvc.Results._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, EitherT, \/-}

/**
  *
  * @param accountService     The account service implementation.
  * @param passwordHasher     The password hasher implementation.
  * @param userService        The user service implementation.
  * @param authInfoRepository The auth info repository implementation.
  */
class SignUpService @Inject()(userService: UserService,
                              accountService: AccountService,
                              roleService: RoleService,
                              forgotPasswordService: ForgotPasswordService,
                              confirmEmailService: ConfirmEmailService,
                              forgotPasswordConfig: ForgotPasswordConfig,
                              authInfoRepository: AuthInfoRepository,
                              passwordHasher: PasswordHasher,
                              messageEmail: MessageEmail
                             ) {

  /**
    * Create a new user and save his account
    *
    * @param loginInfo
    * @param data
    * @param authorizationResult
    * @return
    */
  def signUp(loginInfo: LoginInfo, data: Data, authorizationResult: SignUpsResult): Future[Expect[Account]] = {
    accountService.retrieve(loginInfo).flatMap {
      case Some(user) => Future.successful(-\/(FailError(s"User (${user.user.email}) already exist")))

      case None =>

        val res: Future[Expect[Account]] = {
          val authInfo = passwordHasher.hash(data.password)

          val newUser = UserIn(
            firstName = Some(data.firstName),
            lastName  = Some(data.lastName),
            email     = Some(data.email),
            avatarUrl = None,
            ownedByClient = data.ownedBy
          ).toNotActivatedUser

          signUpFlow(loginInfo, newUser, authInfo, data.email)
        }
        res
    }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }


  private def signUpFlow(loginInfo: LoginInfo, user: User, authInfo: PasswordInfo, email: String): Future[Expect[Account]] = {

    lazy val authInfoRepositoryAdd: Future[Expect[PasswordInfo]] = authInfoRepository.add[PasswordInfo](loginInfo, authInfo).map(e => \/-(e))

    for {
      account <- EitherT(accountService.save(loginInfo, user))
      _       <- EitherT(authInfoRepositoryAdd)
      _       <- EitherT(confirmEmailService.sendEmailConfirmation(email))
    } yield account
  }.run

  def signUpConfirmation(token: String): Future[Expect[User]] = {
    for{
      userOpt <- EitherT(confirmEmailService.checkTokenValidity(token))
      user    <- EitherT(userOpt |> (s"Token [$token] is wrong or expired", BadRequest))
      _       <- EitherT(userService.changeActiveStatus(user.id, status = true))
      _       <- EitherT(grantToAdminFirstUser(user))
    } yield user
  }.run

  def grantToAdminFirstUser(user: User): Future[Expect[Boolean]] = {
    for{
      nbActiveUser  <- EitherT(userService.getNumberActiveUser())
      updated       <- EitherT(updateFirstUserFlow(user,nbActiveUser))
    } yield true
  }.run

  def updateUserRoleFlow(user: User, superadminRoleName : String): Future[Expect[Boolean]] = {
    for{
      idSuperadminRole    <- EitherT(roleService.getIdByRoleName("superadmin"))
      roleIsSet           <- EitherT(roleService.setRoleOptionToUser(idSuperadminRole,user))
    } yield roleIsSet
  }.run

  def updateFirstUserFlow(user: User, nbActiveUser: Int): Future[Expect[Boolean]] = {
    val superadminRoleName : String  = "superadmin"
    nbActiveUser match {
      case 1 => updateUserRoleFlow(user, superadminRoleName)
      case _ => Future.successful(-\/(FailError(s"User:: $user can't be superadmin. Active user number:: $nbActiveUser")))
    }
  }


}