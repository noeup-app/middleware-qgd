package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action
import play.api.mvc._
import SignUpForm.signUpFormDataFormat
import com.noeupapp.middleware.authorizationClient.confirmEmail.{ConfirmEmailForm, ConfirmEmailService}
import com.noeupapp.middleware.authorizationClient.forgotPassword.ForgotPasswordService
import com.noeupapp.middleware.authorizationClient.login.LoginsResult
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.utils.BodyParserHelper._
import com.noeupapp.middleware.utils.RequestHelper

import scala.concurrent.Future
import scalaz.{-\/, \/-}

/**
 * The sign up controller.
 *
 * @param messagesApi The Play messages API.
 */
class SignUps @Inject()( val messagesApi: MessagesApi,
                         val env: Environment[Account, BearerTokenAuthenticator],
                         htmlSignUpsResult: HtmlSignUpsResult,
                         ajaxSignUpsResult: AjaxSignUpsResult,
                         userService: UserService,
                         avatarService: AvatarService,
                         accountService: AccountService,
                         confirmEmailService: ConfirmEmailService,
                         forgotPasswordService: ForgotPasswordService,
                         signUpService: SignUpService) extends Silhouette[Account, BearerTokenAuthenticator] {



  /**
    * Handles the Sign Up action.
    *
    * @return The result to display.
    */
  def signUpActionGet = UserAwareAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        signUp(request, ajaxSignUpsResult)
      case false =>
        signUp(request, htmlSignUpsResult)
    }
  }

  /**
    * Sign up generic process
    *
    * @param request the request
    * @param signUpsResult the implementation of authorizationResult
    * @return The result to return
    */
  def signUp(request: UserAwareRequest[AnyContent], signUpsResult: SignUpsResult): Future[Result] = {
    val req = request.asInstanceOf[signUpsResult.UserAwareRequest[AnyContent]]
    request.identity match {
      case Some(user) => Future.successful(signUpsResult.userIsConnected())
      case None => Future.successful(signUpsResult.userIsNotRegistered(req))
    }
  }



  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def subscribe = Action.async(jsonOrAnyContent[SignUpForm.Data]) { implicit request =>
    RequestHelper.isJson(request) match {
      case true =>
        val data: SignUpForm.Data = request.body.asInstanceOf[SignUpForm.Data]
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        signUp(loginInfo, data, ajaxSignUpsResult)

      case false =>
        SignUpForm.form.bindFromRequest.fold(
          form => Future.successful(htmlSignUpsResult.badRequest(form)),
          data => {
            val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
            signUp(loginInfo, data, htmlSignUpsResult)
          }
        )
    }
  }

  /**
    *
    *
    * @param loginInfo
    * @param data
    * @param authorizationResult
    * @param request
    * @return
    */
  def signUp(loginInfo: LoginInfo, data: SignUpForm.Data, authorizationResult: SignUpsResult)(implicit request: Request[Any]): Future[Result] = {
    accountService.retrieve(loginInfo).flatMap {

      case Some(user) => Future.successful(authorizationResult.userAlreadyExists())

      case None => signUpService.signUp(loginInfo, data, authorizationResult).flatMap {
        case -\/(e) =>

          Logger.error(s"An exception occurred $e")
          Future.successful(authorizationResult.manageError())
        case \/-(account) =>
          for {
            authenticator <- env.authenticatorService.create(loginInfo)
            value         <- env.authenticatorService.init(authenticator)
            result        <- env.authenticatorService.embed(value, authorizationResult.userSuccessfullyCreated())
          } yield {
            Logger.info("User successfully added")
            env.eventBus.publish(SignUpEvent(account, request, request2Messages))
            env.eventBus.publish(LoginEvent(account, request, request2Messages))
            result
          }
      }
    }.recover {
      case e: Exception =>
        Logger.error(s"An exception occurred $e")
        authorizationResult.manageError()
    }
  }



  def emailConfirmation(token: String) = Action.async(jsonOrAnyContent[String]) { implicit request =>
    signUpService.signUpConfirmation(token).map {
      case \/-(u) =>
        Logger.trace("user activated : " + u)
        Ok(com.noeupapp.middleware.authorizationClient.confirmEmail.html.confirmEmail(ConfirmEmailForm.Data("Activated", u)))
      case -\/(error) =>
        Logger.trace("couldn't activate user " + error.message.toString)
        Ok(com.noeupapp.middleware.authorizationClient.confirmEmail.html.confirmEmail(ConfirmEmailForm.Data(error.message.toString, null)))
    }
  }

  /**
    * Resending an email to activate an account (userData.email here)
    * @return
    */
  def resendingEmailConfirmation() = Action.async(parse.form(ConfirmEmailForm.resendingForm)) { implicit request =>
    val userData = request.body
    Logger.trace(s"Resending an email to $userData")
    confirmEmailService.resendingEmail(userData.email).map {
      case \/-(user) => Ok(com.noeupapp.middleware.authorizationClient.confirmEmail.html.confirmEmail(ConfirmEmailForm.Data("Resend", user)))
      case -\/(error) => Ok(com.noeupapp.middleware.authorizationClient.confirmEmail.html.confirmEmail(ConfirmEmailForm.Data(error.message.toString, null)))
    }
  }

  def login(request: UserAwareRequest[AnyContent], loginsResult: LoginsResult): Future[Result] = {
    val req = request.asInstanceOf[loginsResult.UserAwareRequest[AnyContent]]
    request.identity match {
      case Some(user) => Future.successful(loginsResult.userIsConnected())
      case None => Future.successful(loginsResult.userIsNotConnected(req))
    }
  }
}
