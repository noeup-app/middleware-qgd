package com.noeupapp.middleware.entities.account

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json}
import com.noeupapp.middleware.entities.account.Account._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, \/-}

/**
  * The Account controller
  *
  * @param messagesApi The Play messages API.
  * @param env The Silhouette environment.
  */
class Accounts @Inject()(
                       val messagesApi: MessagesApi,
                       val env: Environment[Account, BearerTokenAuthenticator],
                       accountService: AccountService)
  extends Silhouette[Account, BearerTokenAuthenticator] {


  def me = SecuredAction.async { implicit request =>
    accountService.retrieveWithRoles(request.identity.user.email, request.identity.user.id) map {
      case \/-(Some((account, roles))) =>
        val jsonAccount: JsObject = Json.toJson(account).as[JsObject]
        val jsonRoles = Json.toJson(roles)
        Ok(jsonAccount + ("roles" -> jsonRoles))
      case \/-(None) =>
        NotFound("User not found")
      case e @ -\/(_) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
    }
  }

}