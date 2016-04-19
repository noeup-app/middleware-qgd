package com.noeupapp.middleware.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.noeupapp.middleware.authorizationClient.{RoleAuthorization, ScopeAndRoleAuthorization, ScopeAuthorization}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import RoleAuthorization.WithRole
import ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.user.{Account, User}
import com.noeupapp.middleware.utils.RequestHelper

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
                              val env: Environment[Account, BearerTokenAuthenticator],
                              socialProviderRegistry: SocialProviderRegistry,
                              scopeAndRoleAuthorization: ScopeAndRoleAuthorization)
  extends Silhouette[Account, BearerTokenAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
//  def index = SecuredAction.async { implicit request =>
  def index = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("all"))) { implicit request =>
    Ok("Nothing here")
  }
}
