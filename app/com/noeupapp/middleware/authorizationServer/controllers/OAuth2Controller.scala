package qgd.middleware.authorizationServer.controllers

import play.api.mvc.{Action, Controller}
import qgd.middleware.authorizationServer.endpoints.OAuth2TokenEndpoint
import qgd.middleware.authorizationServer.handlers.AuthorizationHandler

import scalaoauth2.provider.OAuth2Provider
import scala.concurrent.ExecutionContext.Implicits.global


class OAuth2Controller extends Controller with OAuth2Provider {
  override val tokenEndpoint = new OAuth2TokenEndpoint()

  def accessToken = Action.async { implicit request =>
    issueAccessToken(new AuthorizationHandler())
  }
}