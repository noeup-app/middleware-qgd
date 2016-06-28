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
                         passwordHasher: PasswordHasher)
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


}
