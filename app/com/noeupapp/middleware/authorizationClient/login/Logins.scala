package com.noeupapp.middleware.authorizationClient.login

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Logger}
import Login.authenticateFormat
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.utils.BodyParserHelper._
import com.noeupapp.middleware.utils.RequestHelper

import scala.concurrent.Future

/**
  * The credentials auth controller.
  *
  * @param messagesApi The Play messages API.
  * @param env The Silhouette environment.
  * @param userService The user service implementation.
  * @param authInfoRepository The auth info repository implementation.
  * @param credentialsProvider The credentials provider.
  * @param socialProviderRegistry The social provider registry.
  * @param configuration The Play configuration.
  * @param clock The clock instance.
  */
class Logins @Inject()(
                        val messagesApi: MessagesApi,
                        val env: Environment[Account, BearerTokenAuthenticator],
                        userService: AccountService,
                        authInfoRepository: AuthInfoRepository,
                        credentialsProvider: CredentialsProvider,
                        socialProviderRegistry: SocialProviderRegistry,
                        htmlLoginsResult: HtmlLoginsResult,
                        ajaxLoginsResult: AjaxLoginsResult,
                        configuration: Configuration,
                        clock: Clock)
  extends Silhouette[Account, BearerTokenAuthenticator] {


  /**
    * Handles the login action.
    *
    * @return The result to display.
    */
  def loginAction = UserAwareAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        login(request, ajaxLoginsResult)
      case false =>
        login(request, htmlLoginsResult)
    }
  }

  /**
    * Login generic process
    *
    * @param request the request
    * @param loginsResult the implementation of authorizationResult
    * @return The result to return
    */
  def login(request: UserAwareRequest[AnyContent], loginsResult: LoginsResult): Future[Result] = {
    val req = request.asInstanceOf[loginsResult.UserAwareRequest[AnyContent]]
    request.identity match {
      case Some(user) => Future.successful(loginsResult.userIsConnected())
      case None => Future.successful(loginsResult.userIsNotConnected(req))
    }
  }



  /**
    * Authenticates a user against the credentials provider.
    *
    * @return The result to display.
    */
  def authenticateAction = Action.async(jsonOrAnyContent[Login]) { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        val authenticateData: Login = request.body.asInstanceOf[Login] // TODO Ugly
        authenticate(authenticateData, ajaxLoginsResult)
      case false =>
        LoginForm.form.bindFromRequest.fold(
          form => Future.successful(htmlLoginsResult.badRequest(form)),
          data => {
            val authenticateData = Login(data.email, data.password, data.rememberMe)
            authenticate(authenticateData, htmlLoginsResult)
          }
        )
    }

  }
  def authenticate(authenticate: Login, loginsResult: LoginsResult)(implicit request: Request[Any]): Future[Result] = {
    val credentials = authenticate.getCredentials
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      val result = loginsResult.userIsAuthenticated()
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          env.authenticatorService.create(loginInfo).map {
            case authenticator if authenticate.rememberMe =>
//              authenticator.copy(
//                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
//                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
//                cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
//              )
              authenticator
            case authenticator => authenticator
          }.flatMap { authenticator =>
            env.eventBus.publish(LoginEvent(user, request, request2Messages))
            env.authenticatorService.init(authenticator).flatMap { v =>
              env.authenticatorService.embed(v, result)
            }
          }
        case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
      }
    }.recover {
      case e: ProviderException =>
        Logger.warn("Logins.authenticate failed : " + authenticate + " -> " + e.getMessage)
        loginsResult.invalidCredentials()
      case e: Exception => {
        Logger.error("An exception ocurred", e)
        loginsResult.manageError(e)
      }
    }
  }


  /**
    * Handles the logout action.
    *
    * @return The result to display.
    */
  def logOut = SecuredAction.async { implicit request =>
    val result = RequestHelper.isJson(request) match {
      case true =>
        ajaxLoginsResult.userLogout()
      case false =>
        htmlLoginsResult.userLogout()
    }
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))
    // TODO Voir si on ne peut pas utiliser ce bus ou reproduire le modele
    env.authenticatorService.discard(request.authenticator, result)
  }

}
