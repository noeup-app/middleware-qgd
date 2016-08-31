package com.noeupapp.middleware.oauth2

import com.google.inject.Inject
import com.noeupapp.middleware.authorizationServer.client.ClientService
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.entities.user.{User, UserService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, \/-}


sealed trait AlternativeFlow

case class AccessTokenAlternativeFlow(value: JsValue) extends AlternativeFlow
case object ClientNotFoundAlternativeFlow extends AlternativeFlow
case object UserNotFoundAlternativeFlow extends AlternativeFlow

/**
  * Created by damien on 27/07/2016.
  */
class AlternativeFlowService @Inject()(alternativeFlowHandler: AlternativeFlowHandler,
                                       oAuthAccessTokenService: OAuthAccessTokenService,
                                       clientService: ClientService,
                                       userService: UserService) {


  def getAlternativeFlowService(alternativeFlowData: AlternativeFlowData): Future[Expect[AlternativeFlow]] = {
    clientService.findByClientIDAndClientSecret(alternativeFlowData.client_id, alternativeFlowData.client_secret)  flatMap {
      case e@ -\/(_) => Future.successful(e)
      case \/-(None) => Future.successful(\/-(ClientNotFoundAlternativeFlow))
      case \/-(Some(client)) => // TODO /!\ WARNING NO SCOPE OR ROLE FILTER
        userService.findByEmail(alternativeFlowData.email, Some(client.clientId)) flatMap {
          case e@ -\/(_) => Future.successful(e)
          case \/-(None) => Future.successful(\/-(UserNotFoundAlternativeFlow))
          case \/-(Some(user)) =>
            alternativeFlowHandler.handle(user, client) map {
              case e@ -\/(_) => e
              case \/-(newToken) =>
                \/-(AccessTokenAlternativeFlow(
                  Json.toJson(AlternativeFlowHandler.grantHandlerToJson(newToken))
                ))
            }

        }
    }

  }

}
