package com.noeupapp.middleware.authorizationClient.signUp

import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.{User, UserService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.MessageEmail
import scala.concurrent.ExecutionContext.Implicits.global
import com.noeupapp.middleware.utils.BooleanCustom._

class ForgotPasswordService @Inject() (messageEmail: MessageEmail, userService: UserService){


  /**
    * The aim of this method
    *
    * @param token
    */
  def checkTokenValidity(token: String): Future[Expect[Option[User]]] = {
    Future.successful(\/-(None))
  }


  def generateAndSaveToken(): Future[Expect[String]] = {
    Future.successful(\/-("token"))
  }


  def sendForgotPasswordEmail(email: String, domain: String): Future[Expect[Unit]] = {
    for{
      token <- EitherT(this.generateAndSaveToken())
      _     <- EitherT(userService.findByEmail(email))
      email <- EitherT{
        val correctDomain = if (domain.endsWith("/")) domain else domain + "/"
        val link = correctDomain + "forgotPassword/" + token
        val content =
           s"""
            |Hello,
            |
            |You asked a new password, click on this link to change your password <a href="$link">$link</a>.
            |
            |If you have not requested a new password, just ignore this mail.
            |
            |Regards,
            |noeup'App team
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
      Logger.error("Email sent")
      ()
    }
  }.run


  def changePassword(token: String, formData: ForgotPasswordAskNewPasswordForm.Data): Future[Expect[Unit]] = {
    for{
      user <- EitherT(this.checkTokenValidity(token))
      _    <- EitherT(user.isDefined |> "This is not user with this email")
      _    <- EitherT(formData.arePasswordsEqual |> "Password are not equals")
      _    <- EitherT(userService.changePassword(user.get.email.get, formData.password))
    } yield ()
  }.run
    .recover{
      case e: Exception => -\/(FailError(e.getMessage, e))
    }



}
