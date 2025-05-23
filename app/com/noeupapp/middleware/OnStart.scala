package com.noeupapp.middleware

import akka.actor.ActorSystem
import com.google.inject.Inject
import com.noeupapp.middleware.notifications.Notification
import com.noeupapp.middleware.notifications.notifiers.{MailerNotificationService, RedisStorageNotificationActor, WebSocketNotificationActor, MailerNotificationActor}
import com.noeupapp.middleware.webSockets.WebSocketAction
import play.api.Logger
import com.noeupapp.middleware.Global._
import org.sedis.Pool

/**
  * Created by damien on 30/05/2017.
  */
class OnStart @Inject()(notification: Notification,
                        pool: Pool,
                        mailerNotificationService: MailerNotificationService) {

  Logger.info("OnStart executing")


  private val webSocketNotificationActor =
    actorSystem.actorOf(
      WebSocketNotificationActor.props(WebSocketAction.webSocketManagerActor),
      "webSocketNotificationActor")

  private val redisStorageNotificationActor =
    actorSystem.actorOf(
      RedisStorageNotificationActor.props(pool),
      "redisStorageNotificationActor")

  private val mailNotificationActor =
    actorSystem.actorOf(
      MailerNotificationActor.props(mailerNotificationService),
      "mailerNotificationActor")

  notification.registerListener(webSocketNotificationActor)
  notification.registerListener(redisStorageNotificationActor)
  notification.registerListener(mailNotificationActor)

}
