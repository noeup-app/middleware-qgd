package com.noeupapp.middleware.oauth2

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}

import com.noeupapp.middleware.oauth2.AlternativeFlowData._
import com.noeupapp.middleware.oauth2.AccessToken._

class TierAccessToken @Inject()(val messagesApi: MessagesApi,
                                val env: Environment[Account, BearerTokenAuthenticator],
                                tierAccessTokenService: TierAccessTokenService
                              ) extends Silhouette[Account, BearerTokenAuthenticator] {



  def getAccessToken = SecuredAction.async { implicit request =>
    val user = request.identity.user
    user.email match {
      case Some(email) =>
        tierAccessTokenService.getAccessTokenFromMiddleTier(email) map {
          case -\/(e) =>
            Logger.error(e.toString)
            InternalServerError("Internal server error")
          case \/-(token) => Ok(Json.toJson(token))
        }
      case None =>
        Logger.error("getAccessToken email is not defined")
        Future.successful(Unauthorized("You are unauthorized"))
    }
  }

}


case class TierAccessTokenConfig(tierName: String,
                                 tierUrl: String,
                                 tierClientId: String,
                                 tierClientSecret: String)


class TierAccessTokenService @Inject()(ws: WSClient,
                                       tierAccessTokenConfig: TierAccessTokenConfig) {

  val TIER_URL = tierAccessTokenConfig.tierUrl

  val TIER_CLIENT_ID     = tierAccessTokenConfig.tierClientId
  val TIER_CLIENT_SECRET = tierAccessTokenConfig.tierClientSecret


  def getAccessTokenFromMiddleTier(email: String): Future[Expect[AccessToken]] = {
    ws
      .url(s"${TIER_URL}oauth2/alternative_access_token")
      .withHeaders("Content-type" -> "application/json")
      .post(
        Json.toJson(
          AlternativeFlowData(
            email,
            TIER_CLIENT_ID,
            TIER_CLIENT_SECRET
          )
        )
      )
      .map(_.json.validate[AccessToken])
      .map {
        case JsSuccess(value, _) => \/-(value)
        case JsError(errors)     => -\/(FailError(errors.mkString(", ")))
      }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }

}