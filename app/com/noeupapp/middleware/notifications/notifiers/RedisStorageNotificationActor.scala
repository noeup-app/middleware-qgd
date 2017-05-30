package com.noeupapp.middleware.notifications.notifiers

import akka.actor.{Actor, Props}
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.notifications.NotificationMessage
import org.sedis.Pool
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import play.api.Logger
import play.api.libs.json.Json

import scalaz.-\/
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 30/05/2017.
  */
class RedisStorageNotificationActor(pool: Pool) extends Actor {

  override def receive: Receive = {
    case notif @ NotificationMessage(user, message_type, message_data) =>
      val key = createKey(user)
      val value: String = notif.asInstanceOf[NotificationMessage[String]]
      FTry(pool.withClient(_.rpush(key, value))) collect {
        case -\/(error) => Logger.error(s"Error while adding notification - $error")
      }
  }

  def createKey(user: User): String = Json.stringify(Json.obj(
    "notification" -> user.id
  ))
}

object RedisStorageNotificationActor {
  def props(pool: Pool) = Props(new RedisStorageNotificationActor(pool))
}


