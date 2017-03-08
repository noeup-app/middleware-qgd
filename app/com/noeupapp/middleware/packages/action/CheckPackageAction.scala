package com.noeupapp.middleware.packages.action

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Authenticator, Environment, Identity, Silhouette}
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.ErrorResult
import com.noeupapp.middleware.packages.PackageHandler
import play.api.{Configuration, Logger}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.Http

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 07/03/2017.
  */
class CheckPackageAction @Inject()(packageHandler: PackageHandler, configuration: Configuration) {


  private def isAuthorized[B](silhouette: Silhouette[Account, CookieBearerTokenAuthenticator])(user: User, request: Request[B]) = {
    packageHandler.isAuthorized(user, request.method, request.uri).map {
      case \/-(_) =>
        None
      case -\/(_) =>
        Logger.warn(s"User ${user.firstName} ${user.lastName} <${user.email}> has not enough credit")
        Some(silhouette.Forbidden(Json.toJson(ErrorResult(403, "Not enough credit"))))
    }
  }


  def checkSecured(silhouette: Silhouette[Account, CookieBearerTokenAuthenticator]) =
    new ActionFunction[silhouette.SecuredRequest, silhouette.SecuredRequest]
      with ActionFilter[silhouette.SecuredRequest] {


      /**
        *
        * RFC rfc7807 quote :
        * For example, consider a response that indicates that the client's
        * account doesn't have enough credit. The 403 Forbidden status code
        * might be deemed most appropriate to use, as it will inform HTTP-
        * generic software (such as client libraries, caches, and proxies) of
        * the general semantics of the response.
        */
      override protected def filter[B](request: silhouette.SecuredRequest[B]): Future[Option[Result]] = {
        isAuthorized(silhouette)(request.identity.user, request)
      }

    }



  def checkUserAware(silhouette: Silhouette[Account, CookieBearerTokenAuthenticator]) =
    new ActionFunction[silhouette.UserAwareRequest, silhouette.UserAwareRequest]
      with ActionFilter[silhouette.UserAwareRequest] {


      /**
        *
        * RFC rfc7807 quote :
        * For example, consider a response that indicates that the client's
        * account doesn't have enough credit. The 403 Forbidden status code
        * might be deemed most appropriate to use, as it will inform HTTP-
        * generic software (such as client libraries, caches, and proxies) of
        * the general semantics of the response.
        */
      override protected def filter[B](request: silhouette.UserAwareRequest[B]): Future[Option[Result]] = {
        request.identity match {
          case Some(account) => isAuthorized(silhouette)(account.user, request)
          case None =>
            Future.successful(Some(
              silhouette.Forbidden(Json.toJson(ErrorResult(403, "User has not package")))))
        }
      }

    }

}

