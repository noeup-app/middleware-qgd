package qgd.middleware.errorHandle

import java.util.Locale
import javax.inject.Inject

import com.mohiva.play.silhouette.api.SecuredErrorHandler
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, Logger, OptionalSourceMapper}

import scala.concurrent.Future

object ErrorHandler {
  case class JsonBadRequest(cause: String)
  implicit val jsonBadRequestFormat = Json.format[JsonBadRequest]
}



/**
  * A secured error handler.
  */
class ErrorHandler @Inject() (
                               env: play.api.Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: javax.inject.Provider[Router])
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
    with SecuredErrorHandler {

  /**
    * Called when a user is not authenticated.
    *
    * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
    *
    * @param request The request header.
    * @param messages The messages for the current language.
    * @return The result to send to the client.
    */
  override def onNotAuthenticated(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    Some(Future.successful(Redirect(qgd.middleware.authorizationClient.controllers.login.routes.Logins.loginAction())))
  }

  /**
    * Called when a user is authenticated but not authorized.
    *
    * As defined by RFC 2616, the status code of the response should be 403 Forbidden.
    *
    * @param request The request header.
    * @param messages The messages for the current language.
    * @return The result to send to the client.
    */
  override def onNotAuthorized(request: RequestHeader, messages: Messages): Option[Future[Result]] = {
    //Some(Future.successful(Redirect(qgd.middleware.authorizationClient.controllers.login.routes.Logins.loginAction()).flashing("error" -> Messages("access.denied")(messages))))
    Some(Future.successful(Forbidden("You are not authorized to access this resource !")))
  }



  /**
    * Invoked when a client makes a bad request.
    *
    * If the request is a json request, then the response is Json too
    *
    * @param request The request that was bad.
    * @param message The error message.
    */
  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Logger.info(request.headers.toString())
    request.headers.get("Content-Type").map(_.toLowerCase(Locale.ENGLISH)) match {
      case Some("application/json") | Some("text/json") =>
        Future.successful(BadRequest(Json.toJson(ErrorHandler.JsonBadRequest(message))))
      case _          =>
        super.onBadRequest(request, message)
    }
  }


}
