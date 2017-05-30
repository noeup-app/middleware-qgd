package com.noeupapp.middleware.notifications.notifiers

import akka.actor.{Actor, Props}
import com.noeupapp.middleware.notifications.NotificationMessage
import com.noeupapp.middleware.webSockets.WebSocketSecurityActor
import play.api.Logger

/**
  * Created by damien on 30/05/2017.
  */
class MailerNotificationActor extends Actor {
  override def receive: Receive = {
    case NotificationMessage(user, message_type, message_data) => Logger.error(s"TODO mail : $user -  $message_type - $message_data")
  }
}

object MailerNotificationActor {
  def props = Props[MailerNotificationActor]
}


