package com.noeupapp.middleware.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.{BearerTokenAuthenticator, CookieAuthenticator}
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.noeupapp.middleware.authorizationClient.{RoleAuthorization, ScopeAndRoleAuthorization, ScopeAuthorization}
import play.api.i18n.MessagesApi
import RoleAuthorization.WithRole
import ScopeAuthorization.WithScope
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.notifications.Notification
import com.noeupapp.middleware.notifications.notifiers.WebSocketNotificationActor
import com.noeupapp.middleware.packages.action.CheckPackageAction



/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
class Applications @Inject()(
                              val messagesApi: MessagesApi,
                              val env: Environment[Account, CookieBearerTokenAuthenticator],
                              socialProviderRegistry: SocialProviderRegistry,
                              scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                              checkPackageAction: CheckPackageAction)
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {


  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = (SecuredAction andThen checkPackageAction.checkSecured(this)) { implicit request =>
    Ok("Nothing here")
  }
}
