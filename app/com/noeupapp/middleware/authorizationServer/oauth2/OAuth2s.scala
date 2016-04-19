package com.noeupapp.middleware.authorizationServer.oauth2

import com.google.inject.Inject
import play.api.mvc.{Action, Controller}
import com.noeupapp.middleware.authorizationServer.endpoints.OAuth2TokenEndpoint

import scala.concurrent.ExecutionContext.Implicits.global
import scalaoauth2.provider.OAuth2Provider


class OAuth2s @Inject() (authorizationHandler: AuthorizationHandler) extends Controller with OAuth2Provider {
  override val tokenEndpoint = new OAuth2TokenEndpoint()

  def accessToken = Action.async { implicit request =>
    issueAccessToken(authorizationHandler)
  }
}