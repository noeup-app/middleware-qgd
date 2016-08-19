package com.noeupapp.middleware.authorizationServer.endpoints

import java.util.UUID

import com.noeupapp.middleware.entities.user.User

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

class OAuth2TokenEndpoint extends TokenEndpoint {

  override val handlers = TokenEndpoint.handlers ++
    Map(
        OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode()
      , OAuthGrantType.REFRESH_TOKEN -> new RefreshToken()
      , OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials()
      , OAuthGrantType.PASSWORD -> new Password()
      , OAuthGrantType.IMPLICIT -> new Implicit()
    )
}
