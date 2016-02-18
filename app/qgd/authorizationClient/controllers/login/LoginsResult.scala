package qgd.authorizationClient.controllers.login

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Request, Result}
import qgd.authorizationClient.controllers.results.AuthorizationResult
import qgd.authorizationClient.controllers.routes
import qgd.authorizationClient.forms.SignInForm
import qgd.resourceServer.models.Account


/**
  * Define results for login controller
  */
trait LoginsResult extends AuthorizationResult {
  def badRequest(form: Form[SignInForm.Data])(implicit request: Request[Any]): Result
  def userIsAuthenticated(): Result
  def invalidCredentials(): Result
  def manageError(e: Exception): Result
}

/**
  * Html results of login controller
  * @param messagesApi
  * @param env
  * @param socialProviderRegistry
  */
class HtmlLoginsResult @Inject() (
                                    val messagesApi: MessagesApi,
                                    val env: Environment[Account, CookieAuthenticator],
                                    socialProviderRegistry: SocialProviderRegistry)
  extends LoginsResult {

  override def badRequest(form: Form[SignInForm.Data])(implicit request: Request[Any]): Result =
    BadRequest(qgd.authorizationClient.views.html.signIn(form, socialProviderRegistry))

  override def userIsAuthenticated(): Result =
    Redirect(routes.ApplicationController.index())

  override def invalidCredentials(): Result =
    Redirect(routes.ApplicationController.signInAction())
      .flashing("error" -> Messages("invalid.credentials"))

  override def manageError(e: Exception): Result =
    Redirect(routes.ApplicationController.signInAction())
      .flashing("error" -> Messages("internal.server.error"))
}


/**
  * Json results of login controller
  * @param messagesApi
  * @param env
  */
class AjaxLoginsResult @Inject() (
                                    val messagesApi: MessagesApi,
                                    val env: Environment[Account, CookieAuthenticator])
  extends LoginsResult {

  override def badRequest(form: Form[SignInForm.Data])(implicit request: Request[Any]): Result =
    BadRequest("Incorrect or incomplete sign in information provided")

  override def userIsAuthenticated(): Result =
    Ok("User is authenticated")

  override def invalidCredentials(): Result =
    Ok("Your credentials are invalid")

  override def manageError(e: Exception): Result =
    InternalServerError(Messages("internal.server.error"))
}
