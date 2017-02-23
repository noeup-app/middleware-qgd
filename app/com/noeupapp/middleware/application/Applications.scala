package com.noeupapp.middleware.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.{BearerTokenAuthenticator, CookieAuthenticator}
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.noeupapp.middleware.authorizationClient.{RoleAuthorization, ScopeAndRoleAuthorization, ScopeAuthorization}
import play.api.i18n.MessagesApi
import RoleAuthorization.WithRole
import ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.account.Account


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
                              scopeAndRoleAuthorization: ScopeAndRoleAuthorization)
  extends Silhouette[Account, CookieAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
//  def index = SecuredAction.async { implicit request =>
//  def index = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("all"))) { implicit request =>
  def index = SecuredAction { implicit request =>
    Ok("Nothing here")
  }
}
