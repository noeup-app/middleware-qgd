package com.noeupapp.middleware.entities.account

import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import com.noeupapp.middleware.entities.account.Account._
import scala.concurrent.ExecutionContext.Implicits.global

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
    val loginInfo = api.LoginInfo("credentials", request.identity.user.email.getOrElse(""))
    accountService.retrieve(loginInfo) map {
      case None => NotFound("Cannot to fetch your data, not found")
      case Some(account) => Ok(Json.toJson(account))
    }
  }

}