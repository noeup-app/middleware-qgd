package qgd.middleware.authorizationServer.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.i18n.MessagesApi
//import models.oauth2.Client
//import play.api.db.slick.DBAction
//import play.api.data._
//import play.api.data.Forms._
//import play.api.Play.current
//import play.api.db.slick.DB
//import oauth2.OAuthDataHandler
//import qgd.middleware.authorizationServer.models
import qgd.middleware.models.Account

class Authorize @Inject()(val messagesApi: MessagesApi,
                          val env: Environment[Account, CookieAuthenticator])
    extends Silhouette[Account, CookieAuthenticator] {


  val log = play.Logger.of("application")

  val errorCodes = Map(
    "access_denied" -> "Access was denied",
    "invalid_request" -> "Request made was not valid",
    "unauthorized_client" -> "Client is not authorized to perform this action",
    "unsupported_response_type" -> "Response type requested is not allowed",
    "invalid_scope" -> "Requested scope is not allowed",
    "server_error" -> "Server encountered an error",
    "temporarily_unavailable" -> "Service is temporary unavailable")

  def authorize = SecuredAction { implicit request =>
    // read URL parameters
    val params = List("client_id", "redirect_uri", "state", "scope")
    val data = params.map(k =>
        k -> request.queryString.getOrElse(k, Seq("")).head).toMap

    val clientId = data("client_id")

    val clientOpt = models.Client.findByClientId(clientId)
    // check if such a client exists
    clientOpt match {

      case None => // doesn't exist
        BadRequest("No such client exists.")

      case Some(client) =>
        val aaInfoForm = AuthorizeForm.form.bind(data)
        log.debug(aaInfoForm.data.toString)
        data.keys.foreach { k =>
          log.debug(k)
          log.debug(aaInfoForm(k).value.toString)
        }
        Ok(views.html.apps.authorize(request.identity, aaInfoForm))
    }
  }

//  def send_auth = withUser { user =>
//    implicit request =>
//      val boundForm = AppAuthInfoForm.bindFromRequest
//      boundForm.fold(
//        formWithErrors => {
//          log.debug(formWithErrors.toString)
//          Ok(views.html.apps.authorize(user, formWithErrors))
//        },
//        aaInfo => {
//          aaInfo.accepted match {
//            case "Y" =>
//              val expiresIn = Int.MaxValue
//              val acOpt =
//                DB.withSession { implicit session =>
//                  models.oauth2.AuthCodes.generateAuthCodeForClient(
//                    aaInfo.clientId, aaInfo.redirectUri, aaInfo.scope,
//                    user.id.get, expiresIn)
//                }
//              acOpt match {
//                case Some(ac) =>
//                  val authCode = ac.authorizationCode
//                  val state = aaInfo.state
//                  Redirect(s"${aaInfo.redirectUri}?code=${authCode}&state=${state}")
//                case None =>
//                  val errorCode = "server_error"
//                  Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
//              }
//
//            case "N" =>
//              val errorCode = "access_denied"
//              Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
//            case _ =>
//              val errorCode = "invalid_request"
//              Redirect(s"${aaInfo.redirectUri}?error=${errorCode}")
//          }
//        })
//  }
}
