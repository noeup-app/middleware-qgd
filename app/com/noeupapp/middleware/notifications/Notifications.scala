package com.noeupapp.middleware.notifications

import java.util.UUID

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, \/-}

/**
  * Created by damien on 31/05/2017.
  */
class Notifications @Inject()(notificationService: NotificationService,
                              val messagesApi: MessagesApi,
                              val env: Environment[Account, CookieBearerTokenAuthenticator])
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {



  def getAllNotifications = SecuredAction.async { implicit request =>
    notificationService.getAllNotifications(request.identity.user.id) map {
      case \/-(notifications) => Ok(Json.toJson(notifications))
      case -\/(error) =>
        Logger.error(s"Notifications error $error")
        InternalServerError("Unable to retrieve notifications")
    }
  }

  def setRead(notifId: UUID) = SecuredAction.async { implicit request =>
    notificationService.setRead(request.identity.user.id, notifId) map {
      case \/-(notifications) => Ok(Json.toJson(notifications))
      case -\/(error) =>
        Logger.error(s"Notifications error $error")
        InternalServerError("Unable to retrieve notifications")
    }
  }

}
