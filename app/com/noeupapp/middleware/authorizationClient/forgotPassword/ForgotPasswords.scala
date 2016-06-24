package com.noeupapp.middleware.authorizationClient.forgotPassword

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.signUp.{AjaxSignUpsResult, HtmlSignUpsResult}
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.utils.BodyParserHelper._
import com.noeupapp.middleware.utils.RequestHelper
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

class ForgotPasswords @Inject()(
                                val messagesApi: MessagesApi,
                                val env: Environment[Account, BearerTokenAuthenticator],
                                userService: UserService,
                                accountService: AccountService,
                                authInfoRepository: AuthInfoRepository,
                                htmlSignUpsResult: HtmlSignUpsResult,
                                ajaxSignUpsResult: AjaxSignUpsResult,
                                avatarService: AvatarService,
                                passwordHasher: PasswordHasher,
                                forgotPasswordService: ForgotPasswordService
                              )
  extends Silhouette[Account, BearerTokenAuthenticator] {

  def forgotPasswordGet() = Action { implicit request =>
    val form = ForgotPasswordForm.form
    Ok(com.noeupapp.middleware.authorizationClient.forgotPassword.html.forgotPassword(form))
  }

  def forgotPasswordAction = Action.async(jsonOrAnyContent[ForgotPasswordForm.Data]) { implicit request =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => {
        Future.successful(
          BadRequest(com.noeupapp.middleware.authorizationClient.forgotPassword.html.forgotPassword(form))
        )
      },
      data => {
        val domain = RequestHelper.getFullDomain
        forgotPasswordService.sendForgotPasswordEmail(data.email, domain) map {
          case -\/(e) =>
            Logger.error(e.toString)
            Redirect(com.noeupapp.middleware.authorizationClient.forgotPassword.routes.ForgotPasswords.forgotPasswordGet())
              .flashing("error" -> "An internal server error occurred. You should try again later.")
          case \/-(_) =>
            Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
              .flashing("info" -> s"An email has been sent to ${data.email}, check your mailbox.")
        }
      }
    )
  }


  def forgotPasswordAskNewPasswordGet(token: String) = Action.async { implicit request =>
    forgotPasswordService.checkTokenValidity(token).map{
      case \/-(Some(_)) =>
        val form = ForgotPasswordAskNewPasswordForm.form
        Ok(com.noeupapp.middleware.authorizationClient.forgotPassword.html.forgotPasswordAskNewPassword(form, token))
      case \/-(None) =>
        Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
          .flashing("error" -> "Page not found. Your token should be expired or already used. please try again.")
      case -\/(_) =>
        Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
          .flashing("error" -> "Internal server error, please try later.")
    }
  }

  def forgotPasswordAskNewPassword(token: String) = Action.async { implicit request =>
    ForgotPasswordAskNewPasswordForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest("BadRequest")),
      data => {
        forgotPasswordService.changePassword(token, data).map{
          case \/-(_) =>
            Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
              .flashing("info" -> "Password successfully changed !")
          case -\/(e) =>
            Logger.error(e.toString)
            Redirect(com.noeupapp.middleware.authorizationClient.forgotPassword.routes.ForgotPasswords.forgotPasswordAskNewPasswordGet(token))
              .flashing("error" -> e.message)
        }
      }
    )
  }

}
