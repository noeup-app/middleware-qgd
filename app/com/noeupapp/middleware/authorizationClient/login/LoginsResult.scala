package com.noeupapp.middleware.authorizationClient.login

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.noeupapp.middleware.authorizationClient.AuthorizationResult
import com.noeupapp.middleware.entities.account.Account
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}


/**
  * Define results for login controller
  */
trait LoginsResult extends AuthorizationResult {
  def badRequest(form: Form[LoginForm.Data])(implicit request: Request[Any]): Result
  def userIsAuthenticated(): Result
  def invalidCredentials(): Result
  def manageError(e: Exception): Result
  def userIsConnected(): Result
  def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result
  def userLogout(): Result
}

/**
  * Html results of login controller
 *
  * @param messagesApi
  * @param env
  * @param socialProviderRegistry
  */
class HtmlLoginsResult @Inject() (
                                    val messagesApi: MessagesApi,
                                    val env: Environment[Account, BearerTokenAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends LoginsResult {

  override def badRequest(form: Form[LoginForm.Data])(implicit request: Request[Any]): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
      .flashing("error" -> "Les identifiants que vous avez envoyé sont dans un format incorrect")

  override def userIsAuthenticated(): Result =
    Redirect(com.noeupapp.middleware.application.routes.Applications.index()) // TODO Check if Result return also a session

  override def invalidCredentials(): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
      .flashing("error" -> Messages("invalid.credentials"))

  override def manageError(e: Exception): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction())
      .flashing("error" -> Messages("internal.server.error"))

  override def userIsConnected(): Result =
    Redirect(com.noeupapp.middleware.application.routes.Applications.index())

  override def userLogout(): Result =
    Redirect(com.noeupapp.middleware.application.routes.Applications.index())

  override def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok(com.noeupapp.middleware.authorizationClient.login.html.login(LoginForm.form, socialProviderRegistry))
}


/**
  * Json results of login controller
 *
  * @param messagesApi
  * @param env
  */
class AjaxLoginsResult @Inject() (
                                    val messagesApi: MessagesApi,
                                    val env: Environment[Account, BearerTokenAuthenticator])
  extends LoginsResult {

  override def badRequest(form: Form[LoginForm.Data])(implicit request: Request[Any]): Result =
    BadRequest("Incorrect or incomplete login information provided")

  override def userIsAuthenticated(): Result =
    Ok("User is authenticated") // TODO should return bearer

  override def invalidCredentials(): Result =
    Ok("Your credentials are invalid")

  override def manageError(e: Exception): Result =
    InternalServerError(Messages("internal.server.error"))

  override def userIsConnected(): Result = Ok("User is connected")

  override def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result = Forbidden("User is not connected")

  override def userLogout(): Result = Ok("User successfully signed out")
}
