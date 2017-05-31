package com.noeupapp.middleware.notifications

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.sedis.Pool
import play.api.Logger

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._

/**
  * Created by damien on 31/05/2017.
  */
class NotificationService @Inject()(pool: Pool,
                                    redisNotificationKeyFactory: RedisNotificationKeyFactory) {

  def getAllNotifications(userId: UUID): Future[Expect[List[String]]] = {
    val notifKey = redisNotificationKeyFactory.createKey(userId)

    FTry(pool.withClient(_.lrange(notifKey, 0, -1)))
  }

  def setRead(userId: UUID, notifId: UUID): Future[Expect[List[String]]] = {

    for{
      notifications <- EitherT(getAllNotifications(userId))
      _             <- EitherT(deleteAllNotifications(userId))

      notificationsWithoutDeleted = notifications.filterNot(_.contains(notifId.toString))

      _ = Logger.info(notifications.toString)
      _ = Logger.info(notifId.toString)
      _ = Logger.info(notificationsWithoutDeleted.toString)

      _             <- EitherT(addAllNotifications(userId, notificationsWithoutDeleted))
    } yield notificationsWithoutDeleted

  }.run

  private def deleteAllNotifications(userId: UUID): Future[Expect[Unit]] = {
    val notifKey = redisNotificationKeyFactory.createKey(userId)
    FTry(pool.withClient(_.del(notifKey)))
  }

  private def addAllNotifications(userId: UUID, notifications: List[String]): Future[Expect[Unit]] = {

    if (notifications.isEmpty) return Future.successful(\/-(()))

    val notifKey = redisNotificationKeyFactory.createKey(userId)
    FTry(pool.withClient(_.rpush(notifKey, notifications: _*)))
  }

}
