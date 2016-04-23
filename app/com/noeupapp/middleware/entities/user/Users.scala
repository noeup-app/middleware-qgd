package com.noeupapp.middleware.entities.user

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.utils.BodyParserHelper._
import com.noeupapp.middleware.utils.RequestHelper
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}

import scala.concurrent.Future
import User._

/**
 * The user controller
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 */
class Users @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, BearerTokenAuthenticator],
                         userService: UserService)
  extends Silhouette[Account, BearerTokenAuthenticator] {


  def me = SecuredAction.async { implicit request =>
    userService.findById(request.identity.user.id) map {
      case None => NotFound("Cannot to fetch your data, not found")
      case Some(user) => Ok(Json.toJson(user))
    }
  }

}
