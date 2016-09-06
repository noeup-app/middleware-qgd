package com.noeupapp.middleware.oauth2

import java.util.UUID

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.{UserIn, UserService}
import com.noeupapp.middleware.errorHandle.ErrorResult
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.BearerTokenGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import org.sedis.Pool
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import com.noeupapp.middleware.oauth2.AlternativeFlowData._
/**
  * Created by damien on 25/07/2016.
  */


case class AlternativeFlowData(email: String,
                               client_id: String,
                               client_secret: String)


object AlternativeFlowData{
  implicit val AlternativeFlowDataFormat = Json.format[AlternativeFlowData]
}

class AlternativeFlows @Inject()(alternativeFlowService: AlternativeFlowService,
                                 val messagesApi: MessagesApi,
                                 val env: Environment[Account, BearerTokenAuthenticator],
                                 scopeAndRoleAuthorization: ScopeAndRoleAuthorization
                                ) extends Silhouette[Account, BearerTokenAuthenticator] {




  def alternativeFlow = Action.async(parse.json[AlternativeFlowData]) { implicit request =>
    val alternativeFlowData = request.body
    alternativeFlowService.getAlternativeFlowService(alternativeFlowData) map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
      case \/-(ClientNotFoundAlternativeFlow)     =>
        Logger.error("Client is not found")
        NotFound(ErrorResult(404, "Client is not found").toJson)
      case \/-(UserNotFoundAlternativeFlow)       =>
        Logger.error("User is not found")
        NotFound(ErrorResult(404, "User is not found").toJson)
      case \/-(AccessTokenAlternativeFlow(token)) => Ok(token)
    }

  }

}
