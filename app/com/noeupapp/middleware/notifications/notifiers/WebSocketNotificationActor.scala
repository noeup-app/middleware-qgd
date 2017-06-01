package com.noeupapp.middleware.notifications.notifiers

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.notifications.NotificationMessage
import com.noeupapp.middleware.webSockets.WebSocketAction.Send
import com.noeupapp.middleware.webSockets.WebSocketManagerActor
import play.api.Logger

/**
  * Created by damien on 30/05/2017.
  */
class WebSocketNotificationActor(webSocketManagerActor: ActorRef) extends Actor {
  override def receive: Receive = {
    case notif @ NotificationMessage(userId, _, _) =>
      webSocketManagerActor ! Send(userId, notif.asInstanceOf[NotificationMessage[String]])
  }
}

object WebSocketNotificationActor {

  def props(webSocketManagerActor: ActorRef) = Props(new WebSocketNotificationActor(webSocketManagerActor))
}
