package qgd.authorizationClient.controllers.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import qgd.authorizationClient.controllers.results.{AjaxAuthorizationResult, AuthorizationResult, HtmlScalaViewAuthorizationResult}
import qgd.resourceServer.models.Account
import qgd.utils.RequestHelper

import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
class Applications @Inject()(
                                        val messagesApi: MessagesApi,
                                        val env: Environment[Account, CookieAuthenticator],
                                        socialProviderRegistry: SocialProviderRegistry,
                                        htmlApplicationsResult: HtmlApplicationsResult,
                                        ajaxApplicationsResult: AjaxApplicationsResult)
  extends Silhouette[Account, CookieAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true =>
        val req = request.asInstanceOf[Applications.this.ajaxApplicationsResult.SecuredRequest[AnyContent]]
        Future.successful(ajaxApplicationsResult.getResource(req))
      case false =>
        val req = request.asInstanceOf[Applications.this.htmlApplicationsResult.SecuredRequest[AnyContent]]
        Future.successful(htmlApplicationsResult.getResource(req))
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
        signIn(request, ajaxApplicationsResult)
      case false =>
        signIn(request, htmlApplicationsResult)
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
  def signUpAction = UserAwareAction.async { implicit request => // TODO : move to signupcontroller
    RequestHelper.isJson(request) match {
      case true  =>
        signUp(request, ajaxApplicationsResult)
      case false =>
        signUp(request, htmlApplicationsResult)
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
        ajaxApplicationsResult.userSignOut()
      case false =>
        htmlApplicationsResult.userSignOut()
    }
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }

  def forgotPassword = Action {
    NotImplemented
  }

}
