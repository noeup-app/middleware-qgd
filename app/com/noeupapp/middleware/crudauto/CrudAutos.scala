package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.crudauto.CrudAuto._
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.http.Writeable
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class CrudAutos @Inject()(crudAutoService: CrudAutoService,
                          val messagesApi: MessagesApi,
                          val env: Environment[Account, BearerTokenAuthenticator],
                          scopeAndRoleAuthorization: ScopeAndRoleAuthorization
                         ) extends Silhouette[Account, BearerTokenAuthenticator] {

  /*def fetchById(model: String, id: UUID) = Action.async { implicit request =>

      crudAutoService.findById(id) map {
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching entity"))
        case \/-(json) =>  Ok(Json.toJson(json))
      }
    }*/

  def fetchName(model: String) = Action.async { implicit request =>

    crudAutoService.getClassName(model) map {
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while fetching entity"))
      case \/-(name) =>  Ok(Json.toJson(name))
    }
  }
}
