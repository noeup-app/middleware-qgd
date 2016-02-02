package qgd.authorizationClient.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.mvc.{Result, AnyContent}
import qgd.authorizationClient.forms._
import models.User
import play.api.i18n.MessagesApi
import qgd.authorizationClient.results.AuthorizationResult

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
  authorizationResult: AuthorizationResult)
  extends Silhouette[User, CookieAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    val req = request.asInstanceOf[ApplicationController.this.authorizationResult.SecuredRequest[AnyContent]]
    Future.successful(authorizationResult.getResource(req))
  }

  /**
   * Handles the Sign In action.
   *
   * @return The result to display.
   */
  def signIn = UserAwareAction.async { implicit request =>
    val req = request.asInstanceOf[ApplicationController.this.authorizationResult.UserAwareRequest[AnyContent]]
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
  def signUp = UserAwareAction.async { implicit request =>
    val req = request.asInstanceOf[ApplicationController.this.authorizationResult.UserAwareRequest[AnyContent]]
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
    val result = authorizationResult.userSignOut()
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }

}
