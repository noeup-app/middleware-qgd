package qgd.middleware.authorizationClient.controllers.application

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import qgd.middleware.models.Account
import qgd.middleware.utils.RequestHelper

import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
class Applications @Inject()(
                                        val messagesApi: MessagesApi,
                                        val env: Environment[Account, CookieAuthenticator],
                                        socialProviderRegistry: SocialProviderRegistry,
                                        htmlApplicationsResult: HtmlApplicationsResult,
                                        ajaxApplicationsResult: AjaxApplicationsResult)
  extends Silhouette[Account, CookieAuthenticator] {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true =>
        val req = request.asInstanceOf[Applications.this.ajaxApplicationsResult.SecuredRequest[AnyContent]]
        Future.successful(ajaxApplicationsResult.getResource(req))
      case false =>
        val req = request.asInstanceOf[Applications.this.htmlApplicationsResult.SecuredRequest[AnyContent]]
        Future.successful(htmlApplicationsResult.getResource(req))
    }
  }
}
