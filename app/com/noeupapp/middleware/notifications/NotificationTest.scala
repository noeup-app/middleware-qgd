package com.noeupapp.middleware.notifications

import java.util.UUID

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}

/**
  * Created by damien on 30/05/2017.
  */
class NotificationTest @Inject()(notification: Notification,
                                 val messagesApi: MessagesApi,
                                 val env: Environment[Account, CookieBearerTokenAuthenticator])
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {

  def test = SecuredAction { implicit request =>
    notification.send(request.identity.user, "etst", UUID.randomUUID().toString)
    Ok("")
  }

}
