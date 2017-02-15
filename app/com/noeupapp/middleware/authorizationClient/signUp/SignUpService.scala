package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.forgotPassword.{ForgotPasswordConfig, ForgotPasswordService}
import com.noeupapp.middleware.authorizationClient.signUp.SignUpForm.Data
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.user.{UserIn, UserService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.MessageEmail
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.utils.FutureFunctor._
import play.api.Logger

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

          confirm   <- sendEmailConfirmation(data.email)
          logger = Logger.debug(s" - - - - - - - Confirmation email $confirm  - - - - - - - -")
        } yield {
          \/-(account)
        }
    }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }


  /**
    * Send an email to allow the user to activate his account
    * @param email user email
    * @return
    */
  def sendEmailConfirmation(email: String): Future[Expect[String]] = {
    val domain = forgotPasswordConfig.url
    for {
      userOpt <- EitherT(userService.findByEmail(email))
      user    <- EitherT(userOpt |> "This is not user with this email")
      token   <- EitherT(forgotPasswordService.generateAndSaveToken(user))
      send    <- EitherT{
        val correctDomain = if (domain.endsWith("/")) domain else domain + "/"
        val link = correctDomain + "signUp/confirmation/" + token
        val content =
          s"""
             |<p>Hello,<p>
             |
             |<p>Please click the link below to activate your account. <a href="$link">$link</a>.</p>
             |
             |<p>This link could be used only during few minutes and once.</p>
          """.stripMargin

        messageEmail.sendEmail(
          senderName = Some("noeup'App"),
          senderEmail = "no-reply@noeupapp.com",
          receiverName = email,
          receiverEmail = email,
          subject = "Account confirmation",
          text = content,
          appName = "noeup'App"
        )
      }
    } yield {
      Logger.info("Account confirmation email sent")
      send
    }
  }.run

}