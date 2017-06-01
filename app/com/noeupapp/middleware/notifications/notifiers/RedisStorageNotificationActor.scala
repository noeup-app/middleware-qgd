package com.noeupapp.middleware.notifications.notifiers

import java.util.UUID

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

  import RedisStorageNotificationActor._

  override def receive: Receive = {
    case notif @ NotificationMessage(notificationId, userId, message_type, message_data) =>
      addNotification(
        createKey(userId),
        NotificationRedis(notificationId, message_type, message_data.toString)
      )
  }

  //TODO duplication
  private def createKey(userId: UUID): String = Json.stringify(Json.obj(
    "notification" -> userId
  ))

  private def addNotification(notifKey: String, notifValue: NotificationRedis) = {

    val valueToString = Json.stringify(Json.toJson(notifValue)(notificationRedisFormat))

    FTry(pool.withClient(_.rpush(notifKey, valueToString))) collect {
      case -\/(error) => Logger.error(s"Error while adding notification - $error")
    }
  }
}

object RedisStorageNotificationActor {

  case class NotificationRedis(id: UUID, message_type: String, message_data: String)

  implicit val notificationRedisFormat = Json.format[NotificationRedis]

  def props(pool: Pool) = Props(new RedisStorageNotificationActor(pool))
}


