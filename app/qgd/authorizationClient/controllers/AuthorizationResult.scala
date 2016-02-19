package qgd.authorizationClient.controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.mvc.Results
import qgd.resourceServer.models.Account


/**
  * Define results (responses HTTP) from the AuthorizationClient
  */
trait AuthorizationResult extends Results with Silhouette[Account, CookieAuthenticator]
