package com.noeupapp.middleware.authorizationClient.socialAuth

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import com.noeupapp.middleware.utils.RequestHelper
import play.api.libs.json.Json

import scala.concurrent.Future

/**
 * The social auth controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 */
class SocialAuths @Inject()(
                             val messagesApi: MessagesApi,
                             val env: Environment[Account, CookieBearerTokenAuthenticator],
                             userService: AccountService,
                             authInfoRepository: AuthInfoRepository,
                             htmlSocialAuthsResult: HtmlSocialAuthsResult,
                             ajaxSocialAuthsResult: AjaxSocialAuthsResult,
                             socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[Account, CookieBearerTokenAuthenticator] with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticateAction(provider: String) = Action.async { implicit request =>
    RequestHelper.isJson(request) match {
      case true  =>
        authenticate(provider, ajaxSocialAuthsResult)
      case false =>
        authenticate(provider, htmlSocialAuthsResult)
    }
  }

  def authenticate(provider: String, authorizationResult: SocialAuthsResult)(implicit request: Request[AnyContent]): Future[Result] = {
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.createOrRetrieve(profile)
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator <- env.authenticatorService.create(profile.loginInfo)
            value <- env.authenticatorService.init(authenticator)
            result <- env.authenticatorService.embed(value, authorizationResult.userSuccessfullyAuthenticated())
          } yield {
            env.eventBus.publish(LoginEvent(user, request, request2Messages))
            logger.debug(s"Authenticate with $provider")
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        authorizationResult.unexpectedProviderError();
    }
  }


  def getProviderIds = Action {
    Ok(Json.toJson(socialProviderRegistry.providers.map(_.id)))
  }
}
