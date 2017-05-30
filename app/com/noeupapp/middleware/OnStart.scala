package com.noeupapp.middleware

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.noeupapp.middleware.notifications.Notification
import com.noeupapp.middleware.notifications.notifiers.WebSocketNotificationActor
import com.noeupapp.middleware.webSockets.WebSocketAction
import play.api.Logger
import com.noeupapp.middleware.Global._

/**
  * Created by damien on 30/05/2017.
  */
class OnStart @Inject()(notification: Notification) {

  Logger.info("OnStart executing")


  private val webSocketNotificationActor =
    actorSystem.actorOf(
      WebSocketNotificationActor.props(WebSocketAction.webSocketManagerActor),
      "webSocketNotificationActor")

  notification.registerListener(webSocketNotificationActor)

}
