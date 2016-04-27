package com.noeupapp.middleware.authorizationClient

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.User
import play.api.mvc.Results


/**
  * Define results (responses HTTP) from the AuthorizationClient
  */
trait AuthorizationResult extends Results with Silhouette[Account, BearerTokenAuthenticator]
