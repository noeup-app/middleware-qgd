package com.noeupapp.middleware.oauth2

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO
import com.noeupapp.middleware.authorizationServer.client.Client
import com.noeupapp.middleware.authorizationServer.oauth2.AuthorizationHandler
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._
import scalaz.{-\/, \/-}


object AlternativeFlowHandler{
  def grantHandlerToJson(r: GrantHandlerResult[_]) =
    Map[String, JsValue](
      "token_type" -> JsString(r.tokenType),
      "access_token" -> JsString(r.accessToken)
    ) ++ r.expiresIn.map {
      "expires_in" -> JsNumber(_)
    } ++ r.refreshToken.map {
      "refresh_token" -> JsString(_)
    } ++ r.scope.map {
      "scope" -> JsString(_)
    }
}

/**
  * Created by damien on 25/07/2016.
  */
class AlternativeFlowHandler @Inject() (passwordHasher: PasswordHasher,
                                        authorizationHandler: AuthorizationHandler,
                                        passwordInfoDAO: PasswordInfoDAO
                                       )
  extends GrantHandler {

  override def handleRequest[U](request: AuthorizationRequest, authorizationHandler: scalaoauth2.provider.AuthorizationHandler[U])(implicit ctx: ExecutionContext): Future[GrantHandlerResult[U]] =
    throw new Exception("AlternativeFlowHandler.handleRequest should not be called")



  def handle(user: User, client: Client): Future[Expect[GrantHandlerResult[User]]] = {
    user.email match {
      case None        =>
        Future.successful(-\/(FailError("user.email is undefined")))
      case Some(email) =>
        val authInfo = AuthInfo[User](user, Some(client.clientId), client.defaultScope, Some(client.redirect_uri))
        issueAccessToken[User](authorizationHandler, authInfo) map (\/-(_))
    }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }

}
