package com.noeupapp.middleware.authorizationClient.signUp

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import SignUpForm.signUpFormDataFormat
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.user.{User, UserIn, UserService}
import com.noeupapp.middleware.utils.BodyParserHelper._
import com.noeupapp.middleware.utils.{BodyParserHelper, RequestHelper}

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}

/**
 * The sign up controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUps @Inject()(
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
  def signUpAction = Action.async(jsonOrAnyContent[SignUpForm.Data]) { implicit request =>
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

  def signUp(loginInfo: LoginInfo, data: SignUpForm.Data, authorizationResult: SignUpsResult)(implicit request: Request[Any]): Future[Result] = {
    accountService.retrieve(loginInfo).flatMap {
      case Some(user) =>
        Future.successful(authorizationResult.userAlreadyExists())
      case None =>
        val authInfo = passwordHasher.hash(data.password)
        val newUser = UserIn(
                          firstName = Some(data.firstName),
                          lastName = Some(data.lastName),
                          email = Some(data.email),
                          avatarUrl = None
                        )
        for {
//          avatar <- avatarService.retrieveURL(data.email)
          user          <- userService.simplyAdd(newUser) // TODO modify simplyAdd and generalise this type off call
          account       <- accountService.save(Account(loginInfo, user, None))
          authInfo      <- authInfoRepository.add(loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(loginInfo)
          value         <- env.authenticatorService.init(authenticator)
          result        <- env.authenticatorService.embed(value, authorizationResult.userSuccessfullyCreated())
        } yield {
          Logger.info("User successfully added")
          env.eventBus.publish(SignUpEvent(account, request, request2Messages))
          env.eventBus.publish(LoginEvent(account, request, request2Messages))
          result
        }
    }.recover{
      case e: Exception => {
        Logger.error("An exception occurred", e)
        authorizationResult.manageError(e)
      }
    }
  }

  def forgotPasswordGet() = Action { implicit request =>
    val form = ForgotPasswordForm.form
    Ok(com.noeupapp.middleware.authorizationClient.signUp.html.forgotPassword(form))
  }

  def forgotPasswordAction = Action.async(jsonOrAnyContent[ForgotPasswordForm.Data]) { implicit request =>
      ForgotPasswordForm.form.bindFromRequest.fold(
        form => {
          Future.successful(
            BadRequest(com.noeupapp.middleware.authorizationClient.signUp.html.forgotPassword(form))
          )
        },
        data => {
          val domain = RequestHelper.getFullDomain
          forgotPasswordService.sendForgotPasswordEmail(data.email, domain) map {
            case -\/(e) =>
              Logger.error(e.toString)
              Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.forgotPasswordGet())
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
        Ok(com.noeupapp.middleware.authorizationClient.signUp.html.forgotPasswordAskNewPassword(form, token))
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
        /*forgotPasswordService.checkTokenValidity(token).map{
          case \/-(Some(user)) =>
            if(data.isPasswordEquals){
              userService.changePassword(user.email.getOrElse(""), data.password).map{
                case \/-(_) =>
                  Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
                    .flashing("info" -> "Password successfully changed !")
                case -\/(e) =>
                  Logger.error(e.toString)
                  Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.forgotPasswordAskNewPasswordGet(token))
                    .flashing("error" -> "An error occured. Please try again")
              }
            }else{
              Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.forgotPasswordAskNewPasswordGet(token))
                .flashing("error" -> "Passwords are different")
            }
          case \/-(None) =>
            Future.successful(NotFound("Page not found. Your token should be expired. Try again."))
          case -\/(_) =>
            Future.successful(InternalServerError("InternalServerError"))
        }*/

        forgotPasswordService.changePassword(token, data).map{
          case \/-(_) =>
            Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
              .flashing("info" -> "Password successfully changed !")
          case -\/(e) =>
            Logger.error(e.toString)
            Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.forgotPasswordAskNewPasswordGet(token))
              .flashing("error" -> e.message)
        }
      }
    )
  }


}
