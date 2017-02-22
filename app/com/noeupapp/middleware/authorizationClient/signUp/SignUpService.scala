package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmailService
import com.noeupapp.middleware.authorizationClient.forgotPassword.{ForgotPasswordConfig, ForgotPasswordService}
import com.noeupapp.middleware.authorizationClient.signUp.SignUpForm.Data
import com.noeupapp.middleware.entities.account.{Account, AccountService}
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
  def signUp(loginInfo: LoginInfo, data: Data, authorizationResult: SignUpsResult): Future[Expect[(Account)]] = {
    accountService.retrieve(loginInfo).flatMap {
      case Some(user) => Future.successful(-\/(FailError("User already exist")))

      case None =>
        val authInfo = passwordHasher.hash(data.password)
        val newUser = UserIn(
          firstName = Some(data.firstName),
          lastName  = Some(data.lastName),
          email     = Some(data.email),
          avatarUrl = None
        )
        for {
        //          avatar <- avatarService.retrieveURL(data.email)

        //          user      <- userService.simplyAdd(newUser) // TODO modify simplyAdd and generalise this type off call
          user      <- userService.addInactive(newUser) // TODO modify simplyAdd and generalise this type off call

          account   <- accountService.save(Account(loginInfo, user, None))
          _         <- authInfoRepository.add(loginInfo, authInfo)

          confirm   <- confirmEmailService.sendEmailConfirmation(data.email)
        } yield {
          \/-(account)
        }
    }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }

  def signUpConfirmation(token: String): Future[Expect[User]] = {
    for{
      userOpt <- EitherT(confirmEmailService.checkTokenValidity(token))
      user    <- EitherT(userOpt |> (s"Wrong token or expired: $userOpt", BadRequest))
      _       <- EitherT(userService.changeActiveStatus(user.id, true))
    } yield user
  }.run

}