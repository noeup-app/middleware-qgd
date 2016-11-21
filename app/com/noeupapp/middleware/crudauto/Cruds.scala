package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class Cruds @Inject()(crudService: CrudService,
                      val messagesApi: MessagesApi,
                      val env: Environment[Account, BearerTokenAuthenticator],
                      scopeAndRoleAuthorization: ScopeAndRoleAuthorization
                         ) extends Silhouette[Account, BearerTokenAuthenticator] {

//  def fetchById(model: String, id: UUID/*, omit: Option[String], include: Option[String]*/) = UserAwareAction.async { implicit request =>
//
////    val omits    = omit.map(_.split(",").toList)
////    val includes = include.map(_.split(",").toList)
//
//    crudService.findByIdFlow(model, id) map {
//      case -\/(error) =>
//        Logger.error(error.toString)
//        InternalServerError(Json.toJson("Error while fetching "+model))
//      case \/-(json) =>  Ok(Json.toJson(json))
//    }
//  }

  def fetchAll(model: String) = UserAwareAction.async { implicit request =>

    crudService.findAllFlow(model) map {
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while fetching "+model))
      case \/-(json) =>  Ok(Json.toJson(json))
    }
  }
//
//  def fetchName(model: String) = UserAwareAction.async { implicit request =>
//
//    crudService.getClassName(model) map {
//      case -\/(error) =>
//        Logger.error(error.toString)
//        InternalServerError(Json.toJson("Error while fetching "+model))
//      case \/-(name) =>  Ok(Json.toJson(name))
//    }
//  }
//
//  def add(model: String) = UserAwareAction.async(parse.json) { implicit request =>
//
//    val json = request.body.as[JsObject]
//    crudService.addFlow(model, json) map {
//      case -\/(error) => error.message match {
//        case m if m.contains("validating json") =>
//          Logger.error(error.toString)
//          BadRequest(m)
//        case _ =>
//          Logger.error(error.toString)
//          InternalServerError(Json.toJson("Error while adding new "+model))
//      }
//      case \/-(js) =>  Ok(Json.toJson(js))
//    }
//  }
//
//  def update(model: String, id: UUID) = UserAwareAction.async(parse.json) { implicit request =>
//
//    val json = request.body.as[JsObject]
//    crudService.updateFlow(model, json, id) map {
//      case -\/(error) =>
//        error.message match {
//          case m if m.contains("fields") =>
//            Logger.error(error.toString)
//            BadRequest(m)
//          case _ =>
//            Logger.error(error.toString)
//            InternalServerError(Json.toJson("Error while adding new "+model))
//        }
//      case \/-(js) =>  Ok(Json.toJson(js))
//    }
//  }
//
//  def delete(model: String, id: UUID, purge:Option[Boolean]) = UserAwareAction.async { implicit request =>
//
//    crudService.deleteFlow(model, id, purge) map {
//      case -\/(error) =>
//        Logger.error(error.toString)
//        InternalServerError(Json.toJson("Error while deleting "+model))
//      case \/-(true) => Ok(Json.toJson("deletion successful"))
//      case \/-(false) => InternalServerError("deletion failed")
//    }
//  }
}
