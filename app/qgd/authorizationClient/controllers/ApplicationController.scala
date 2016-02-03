package qgd.authorizationClient.controllers

import java.util.Locale
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.libs.json.{Reads, Json}
import play.api.mvc.{Action, Result, AnyContent}
import qgd.authorizationClient.forms._
import qgd.authorizationClient.models.User
import play.api.i18n.MessagesApi
import qgd.authorizationClient.results.{AjaxAuthorizationResult, HtmlScalaViewAuthorizationResult, AuthorizationResult}
import qgd.authorizationClient.utils.BodyParserHelper._
import qgd.authorizationClient.utils.RequestHelper

import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
class ApplicationController @Inject() (
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry,
  htmlScalaViewAuthorizationResult: HtmlScalaViewAuthorizationResult,
  ajaxAuthorizationResult: AjaxAuthorizationResult)
  extends Silhouette[User, CookieAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true =>
        val req = request.asInstanceOf[ApplicationController.this.ajaxAuthorizationResult.SecuredRequest[AnyContent]]
        Future.successful(ajaxAuthorizationResult.getResource(req))
      case false =>
        val req = request.asInstanceOf[ApplicationController.this.htmlScalaViewAuthorizationResult.SecuredRequest[AnyContent]]
        Future.successful(htmlScalaViewAuthorizationResult.getResource(req))
    }
  }

  /**
   * Handles the Sign In action.
   *
   * @return The result to display.
   */
  def signInAction = UserAwareAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        signIn(request, ajaxAuthorizationResult)
      case false =>
        signIn(request, htmlScalaViewAuthorizationResult)
    }
  }

  /**
    * Sign in generic process
    *
    * @param request the request
    * @param authorizationResult the implementation of authorizationResult
    * @return The result to return
    */
  def signIn(request: UserAwareRequest[AnyContent], authorizationResult: AuthorizationResult): Future[Result] = {
    val req = request.asInstanceOf[authorizationResult.UserAwareRequest[AnyContent]]
    request.identity match {
      case Some(user) => Future.successful(authorizationResult.userIsConnected())
      case None => Future.successful(authorizationResult.userIsNotConnected(req))
    }
  }

  /**
   * Handles the Sign Up action.
   *
   * @return The result to display.
   */
  def signUpAction = UserAwareAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        signUp(request, ajaxAuthorizationResult)
      case false =>
        signUp(request, htmlScalaViewAuthorizationResult)
    }
  }

  /**
    * Sign up generic process
    *
    * @param request the request
    * @param authorizationResult the implementation of authorizationResult
    * @return The result to return
    */
  def signUp(request: UserAwareRequest[AnyContent], authorizationResult: AuthorizationResult): Future[Result] = {
    val req = request.asInstanceOf[authorizationResult.UserAwareRequest[AnyContent]]
    request.identity match {
      case Some(user) => Future.successful(authorizationResult.userIsConnected())
      case None => Future.successful(authorizationResult.userIsNotRegistered(req))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val result = RequestHelper.isJson(request) match {
      case true =>
        ajaxAuthorizationResult.userSignOut()
      case false =>
        htmlScalaViewAuthorizationResult.userSignOut()
    }
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }

}
