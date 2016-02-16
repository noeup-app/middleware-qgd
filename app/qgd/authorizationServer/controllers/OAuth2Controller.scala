package qgd.authorizationServer.controllers

import play.api.mvc.{Action, Controller}
import qgd.authorizationServer.endpoints.OAuth2TokenEndpoint
import qgd.authorizationServer.handlers.AuthorizationHandler

import scalaoauth2.provider.OAuth2Provider
import scala.concurrent.ExecutionContext.Implicits.global


object OAuth2Controller extends Controller with OAuth2Provider {
  override val tokenEndpoint = new OAuth2TokenEndpoint()

  def accessToken = Action.async { implicit request =>
    issueAccessToken(new AuthorizationHandler())
  }
}