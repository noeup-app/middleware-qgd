package com.noeupapp.middleware.authorizationClient.socialAuth

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.noeupapp.middleware.authorizationClient.AuthorizationResult
import com.noeupapp.middleware.entities.user.{Account, User}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Result

/**
  * Define results for SocialAuths
  */
trait SocialAuthsResult extends AuthorizationResult {
  def userSuccessfullyAuthenticated(): Result
  def unexpectedProviderError(): Result
}

/**
  * Define HTML results for SocialAuths
 *
  * @param messagesApi
  * @param env
  */
class HtmlSocialAuthsResult @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[Account, BearerTokenAuthenticator])
  extends SocialAuthsResult {

  override def userSuccessfullyAuthenticated(): Result =
    Redirect(com.noeupapp.middleware.application.routes.Applications.index())

  override def unexpectedProviderError(): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
      .flashing("error" -> Messages("could.not.authenticate"))
}


/**
  * Define Json results for SocialAuths
 *
  * @param messagesApi
  * @param env
  */
class AjaxSocialAuthsResult @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[Account, BearerTokenAuthenticator])
  extends SocialAuthsResult {

  override def userSuccessfullyAuthenticated(): Result = Ok("User successfully authenticated") // TODO Doublon avec Logins?

  override def unexpectedProviderError(): Result = InternalServerError("Unexpected error occurred")
}
