package qgd.authorizationClient.controllers.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.i18n.MessagesApi
import play.api.mvc.{Result, AnyContent}
import qgd.authorizationClient.controllers.AuthorizationResult
import qgd.resourceServer.models.Account

/**
  * Define results for Application controller
  */
trait ApplicationsResult extends AuthorizationResult {
  def getResource(implicit request: SecuredRequest[AnyContent]): Result
}

/**
  * Define HTML results for Application controller
  * @param messagesApi
  * @param env
  */
class HtmlApplicationsResult @Inject() (
                                         val messagesApi: MessagesApi,
                                         val env: Environment[Account, CookieAuthenticator])
  extends ApplicationsResult {

  override def getResource(implicit request: SecuredRequest[AnyContent]): Result =
    Ok(qgd.authorizationClient.views.html.ressource(Some(request.identity)))
}

/**
  * Define Json results for Application controller
  * @param messagesApi
  * @param env
  */
class AjaxApplicationsResult @Inject() (
                                         val messagesApi: MessagesApi,
                                         val env: Environment[Account, CookieAuthenticator])
  extends ApplicationsResult {

  override def getResource(implicit request: SecuredRequest[AnyContent]): Result = Ok("Resource")
}