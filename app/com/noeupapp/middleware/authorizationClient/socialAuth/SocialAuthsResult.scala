package qgd.middleware.authorizationClient.socialAuth

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Result
import qgd.middleware.authorizationClient.controllers.AuthorizationResult
import qgd.middleware.models.Account

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
                                        val env: Environment[Account, CookieAuthenticator])
  extends SocialAuthsResult {

  override def userSuccessfullyAuthenticated(): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.application.routes.Applications.index())

  override def unexpectedProviderError(): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.login.routes.Logins.loginAction())
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
                                        val env: Environment[Account, CookieAuthenticator])
  extends SocialAuthsResult {

  override def userSuccessfullyAuthenticated(): Result = Ok("User successfully authenticated") // TODO Doublon avec Logins?

  override def unexpectedProviderError(): Result = InternalServerError("Unexpected error occurred")
}
