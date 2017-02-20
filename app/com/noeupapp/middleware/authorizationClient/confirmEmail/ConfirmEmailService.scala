package com.noeupapp.middleware.authorizationClient.confirmEmail

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
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmail._


class ConfirmEmailService @Inject() (pool: Pool,
                                     userService: UserService,
                                     messageEmail: MessageEmail,
                                     confirmEmailConfig: ConfirmEmailConfig) extends CaseClassUtils {


    def generateAndSaveToken(user: User): Future[Expect[String]] = {
      val token = BearerTokenGenerator.generateToken(confirmEmailConfig.tokenLength)
      try{
        pool.withClient(_.set(ConfirmEmailKey(token), user))
        pool.withClient(_.expire(ConfirmEmailKey(token), confirmEmailConfig.tokenExpiresInSeconds))
        Future.successful(\/-(token))
      }catch {
        case e: Exception =>
          val failError = FailError(e.getMessage, e)
          Logger.error("ConfirmEmailService.generateAndSaveToken " + failError.toString)
          Future.successful(-\/(failError))
      }
    }

    def checkTokenValidity(token: String): Future[Expect[Option[User]]] = Future {
      Try{
        pool.withClient(_.get(ConfirmEmailKey(token))) flatMap (r => stringToCaseClass[User](r))
      } match {
        case res @ \/-(_) => res
        case error @ -\/(e) =>
          Logger.debug("ConfirmEmailService.checkTokenValidity " + error +  "  "+ e.toString)
          Logger.error("ConfirmEmailService.checkTokenValidity" + e.toString)
          error
      }
    }

    def dropToken(token: String): Future[Expect[Unit]] = Future {
      val key: String = ConfirmEmailKey(token)
      Try{
        pool.withClient(_.del(key))
      } match {
        case \/-(_) => \/-(())
        case error @ -\/(e) =>
          Logger.error("ConfirmEmailService.dropToken" + e.toString)
          error
      }
    }

  /**
    * Send an email to allow the user to activate his account
    * @param email user email
    * @return
    */
  def sendEmailConfirmation(email: String): Future[Expect[String]] = {
    val domain = confirmEmailConfig.url
    for {
      userOpt <- EitherT(userService.findByEmail(email))
      user    <- EitherT(userOpt |> "This is not user with this email")
      token   <- EitherT(generateAndSaveToken(user))
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

