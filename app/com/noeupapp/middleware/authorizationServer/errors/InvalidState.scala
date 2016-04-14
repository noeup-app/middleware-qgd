package qgd.middleware.authorizationServer.errors

import scalaoauth2.provider.OAuthError

class InvalidState(description: String = "") extends OAuthError(description) {
  override val errorType = "invalid_state"
}