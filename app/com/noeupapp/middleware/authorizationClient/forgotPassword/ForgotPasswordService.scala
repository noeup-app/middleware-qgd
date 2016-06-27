package com.noeupapp.middleware.authorizationClient.forgotPassword

import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.User.UserFormat
import com.noeupapp.middleware.entities.user.{User, UserService}
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.utils.{BearerTokenGenerator, CaseClassUtils, MessageEmail}
import org.sedis.Pool
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.authorizationClient.forgotPassword.ForgotPassword._
import com.noeupapp.middleware.entities.user.User._


class ForgotPasswordService @Inject() (messageEmail: MessageEmail,
                                       userService: UserService,
                                       pool: Pool,
                                       forgotPasswordConfig: ForgotPasswordConfig) extends CaseClassUtils{



  def generateAndSaveToken(user: User): Future[Expect[String]] = {
    val token = BearerTokenGenerator.generateToken(forgotPasswordConfig.tokenLength)
    try{
      pool.withClient(_.set(ForgotPasswordKey(token), user))
      pool.withClient(_.expire(ForgotPasswordKey(token), forgotPasswordConfig.tokenExpiresInSeconds))
      Future.successful(\/-(token))
    }catch {
      case e: Exception =>
        val failError = FailError(e.getMessage, e)
        Logger.error("ForgotPasswordService.generateAndSaveToken " + failError.toString)
        Future.successful(-\/(failError))
    }
  }

  def checkTokenValidity(token: String): Future[Expect[Option[User]]] = Future {
    Try{
      pool.withClient(_.get(ForgotPasswordKey(token))) flatMap (r => stringToCaseClass[User](r))
    } match {
      case res @ \/-(_) => res
      case error @ -\/(e) =>
        Logger.error("ForgotPasswordService.checkTokenValidity" + e.toString)
        error
    }
  }

  def dropToken(token: String): Future[Expect[Unit]] = Future {
    val key: String = ForgotPasswordKey(token)
    Try{
      pool.withClient(_.del(key))
    } match {
      case \/-(_) => \/-(())
      case error @ -\/(e) =>
        Logger.error("ForgotPasswordService.dropToken" + e.toString)
        error
    }
  }




  def sendForgotPasswordEmail(email: String, domain: String, prefix: String = ""): Future[Expect[Unit]] = {
    for{
      userOpt <- EitherT(userService.findByEmail(email))
      user    <- EitherT(userOpt |> "This is not user with this email")
      token   <- EitherT(this.generateAndSaveToken(user))
      email   <- EitherT{
        val correctDomain = if (domain.endsWith("/")) domain else domain + "/"
        val link = correctDomain + prefix + "forgotPassword/" + token
        val content =
           s"""
            |<p>Hello,<p>
            |
            |<p>You asked a new password, click on this link to change your password <a href="$link">$link</a>.</p>
            |
            |<p>This link could be used only during few minutes and once.</p>
            |
            |<p>If you have not requested a new password, just ignore this mail.</p>
          """.stripMargin

        messageEmail.sendEmail(
          senderName = Some("noeup'App"),
          senderEmail = "no-reply@noeupapp.com",
          receiverName = email,
          receiverEmail = email,
          subject = "Password recovery",
          text = content,
          appName = "noeup'App"
        )
      }
    } yield {
      Logger.info("Password recovery email sent")
      ()
    }
  }.run


  def changePassword(token: String, formData: ForgotPasswordAskNewPasswordForm.Data): Future[Expect[Unit]] = {
    for{
      user  <- EitherT(this.checkTokenValidity(token))
      _     <- EitherT(user.isDefined |> "This is not user with this email")
      _     <- EitherT(formData.arePasswordsEqual |> "Password are not equals")
      email <- EitherT(user.get.email |> "Internal server error")
      _     <- EitherT(userService.changePassword(email, formData.password))
      _     <- EitherT(this.dropToken(token))
    } yield ()
  }.run



}
