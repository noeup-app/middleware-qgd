package com.noeupapp.middleware.crudauto

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._

class Cruds @Inject()(crudService: AbstractCrudService,
                      val messagesApi: MessagesApi,
                      val env: Environment[Account, CookieBearerTokenAuthenticator],
                      scopeAndRoleAuthorization: ScopeAndRoleAuthorization
                     ) extends Silhouette[Account, CookieBearerTokenAuthenticator] {

  def fetchById(model: String,
                id: String,
                omit: Option[String],
                include: Option[String]) = UserAwareAction.async { implicit request =>

    val omits    = omit.map(_.split(",").toList).toList.flatten
    val includes = include.map(_.split(",").toList).toList.flatten

    crudService.findByIdFlow(model, id, omits, includes, request.identity.map(_.user)) map {
      case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
        Logger.warn(s"Unauthorized GET /$model/$id")
        Unauthorized(Json.toJson("Unauthorized"))
      case -\/(error) if error.errorType.header.status == BadRequest.header.status =>
        Logger.warn(error.toString)
        BadRequest(Json.toJson(s"id type given is not correct (id given is `$id`)"))
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while fetching "+model))
      case \/-(Some(json)) =>  Ok(Json.toJson(json))
      case \/-(None)       =>  NotFound("Entity not found")
    }
  }

  def fetchAll(model: String,
               omit: Option[String],
               include: Option[String],
               search: Option[String] = None,
               count: Option[Boolean] = Some(false),
               p: Option[Int] = None,
               pp: Option[Int] = None,
               withDelete: Option[Boolean]) = UserAwareAction.async { implicit request =>


    if (p.isDefined ^ pp.isDefined) {
      Logger.error("One of query param `p` or `pp` is missing")
      Future.successful(BadRequest(Json.toJson("One of query param `p` or `pp` is missing")))
    } else {
      val omits = omit.map(_.split(",").toList).toList.flatten
      val includes = include.map(_.split(",").toList).toList.flatten

      crudService.findAllFlow(model, omits, includes, search, count.getOrElse(false), p, pp, request.identity, withDelete) map {
        case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
          Logger.warn(s"Unauthorized GET /$model")
          Unauthorized(Json.toJson("Unauthorized"))
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson("Error while fetching " + model))
        case \/-(json) => Ok(Json.toJson(json))
      }
    }
  }


  def deepFetchAll(model1: String,
                   id: String,
                   model2: String,
                   omit: Option[String],
                   include: Option[String],
                   search: Option[String] = None,
                   count: Option[Boolean] = Some(false),
                   p: Option[Int] = None,
                   pp: Option[Int] = None,
                   withDelete: Option[Boolean]) = UserAwareAction.async { implicit request =>

    if (p.isDefined ^ pp.isDefined) {
      Logger.error("One of query param `p` or `pp` is missing")
      Future.successful(BadRequest(Json.toJson("One of query param `p` or `pp` is missing")))
    } else {
      val omits = omit.map(_.split(",").toList).toList.flatten
      val includes = include.map(_.split(",").toList).toList.flatten

      crudService.deepFetchAllFlow(model1, id, model2, omits, includes, search, count.getOrElse(false), p, pp, request.identity, withDelete) map {
        case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
          Logger.warn(s"Unauthorized GET /$model1/$id/$model2")
          Unauthorized(Json.toJson("Unauthorized"))
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError(Json.toJson(s"Error while fetching /$model1/$id/$model2"))
        case \/-(None) => NotFound(Json.toJson(s"`/$model1/$id` is not found"))
        case \/-(json) => Ok(Json.toJson(json))
      }
    }

  }

  def deepFetchById(model1: String,
                    id1: String,
                    model2: String,
                    id2: String,
                    omit: Option[String],
                    include: Option[String]) = UserAwareAction.async { implicit request =>

    val omits    = omit.map(_.split(",").toList).toList.flatten
    val includes = include.map(_.split(",").toList).toList.flatten

    crudService.deepFetchByIdFlow(model1, id1, model2, id2, omits, includes, request.identity.map(_.user)) map {
      case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
        Logger.warn(s"Unauthorized GET /$model1/$id1/$model2/$id2")
        Unauthorized(Json.toJson("Unauthorized"))
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson(s"Error while fetching /$model1/$id1/$model2/$id2"))
      case \/-(None) => NotFound(Json.toJson(s"`/model1/$id1/$model2/$id2` is not found"))
      case \/-(json) => Ok(Json.toJson(json))
    }
  }

  def add(model: String) = UserAwareAction.async(parse.json) { implicit request =>

    val json = request.body.as[JsObject]
    crudService.addFlow(model, json, request.identity.map(_.user)) map {
      case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
        Logger.warn(s"Unauthorized POST /$model")
        Unauthorized(Json.toJson("Unauthorized"))
      case -\/(error) if error.errorType.header.status == BadRequest.header.status =>
        Logger.error(error.toString)
        BadRequest(Json.toJson("Json given is not correct"))
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while adding new " + model))
      case \/-(js) =>  Ok(Json.toJson(js))
    }
  }

  def update(model: String, id: String) = UserAwareAction.async(parse.json) { implicit request =>

    val json = request.body.as[JsObject]
    crudService.updateFlow(model, json, id, request.identity.map(_.user)) map {
      case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
        Logger.warn(s"Unauthorized PUT /$model")
        Unauthorized(Json.toJson("Unauthorized"))
      case -\/(error) if error.errorType.header.status == BadRequest.header.status =>
        BadRequest(Json.toJson("Json given is not correct"))
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while updating " + model))
      case \/-(Some(js)) =>  Ok(Json.toJson(js))
      case \/-(None)     =>  NotFound(Json.toJson(s"/$model/$id is not found"))
    }
  }

  def delete(model: String, id: String, purge:Option[Boolean], force_delete: Option[Boolean] = None) = UserAwareAction.async { implicit request =>

    crudService.deleteFlow(model, id, purge, force_delete.getOrElse(false), request.identity.map(_.user)) map {
      case -\/(error) if error.errorType.header.status == Unauthorized.header.status =>
        Logger.warn(s"Unauthorized DELETE /$model")
        Unauthorized(Json.toJson("Unauthorized"))
      case -\/(error) =>
        Logger.error(error.toString)
        InternalServerError(Json.toJson("Error while deleting "+model))
      case \/-(Some(json)) =>  Ok(Json.toJson(json))
      case \/-(None)       =>  NotFound("Entity not found")
    }
  }
}
