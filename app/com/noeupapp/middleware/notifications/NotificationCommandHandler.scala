package com.noeupapp.middleware.notifications

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.webSockets.WebSocketAction.Quit
import play.api.Logger
import play.api.libs.json.Json
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.ExecutionContext.Implicits.global
import com.noeupapp.middleware.utils.FutureFunctor._
import org.sedis.Pool

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}

/**
  * Created by damien on 30/05/2017.
  */
class NotificationCommandHandler(userId: UUID, out: ActorRef, manager: ActorRef, pool: Pool) extends Actor {

  import NotificationCommandHandler._

  override def receive: Receive = {
    case message: String =>

      Logger.info(s"NotificationCommandHandler $message")

      {
        for{
          command  <- EitherT(parseCommand(message))
          response <- EitherT(handleCommand(command))
        } yield out ! response
      }.run collect {
        case -\/(error) => out ! returnError(message)
      }

  }

  def parseCommand(command: String): Future[Expect[Command]] =
    FTry(Json.parse(command).validate[Command](commandFormat).get)

  def handleCommand(command: Command): Future[Expect[String]] = {
    command match {
      case Command(COMMAND_SET_READ, Some(notifId)) =>
        Future.successful(\/-(setDeleted(userId, notifId)))
      case Command(COMMAND_LIST_ALL, _) =>
        Future.successful(\/-(retrieveInRedis(userId)))
      case error => Future.successful(\/-(returnError(error)))
    }
  }

  def returnError(command: Command): String =
    Json.stringify(Json.obj("message_type" -> "error", "message_data" -> s"Command ($command) not found"))

  def returnError(command: String): String =
    Json.stringify(Json.obj("message_type" -> "error", "message_data" -> s"Command ($command) not found"))


  // TODO duplication
  private def createKey(userId: UUID): String = Json.stringify(Json.obj(
    "notification" -> userId
  ))

  def setDeleted(userId: UUID, notifId: String): String = {

    Try(pool.withClient(_.lrange(createKey(userId), 0, -1))) match {
      case \/-(notifs: List[String]) =>

        val elementDeleted = notifs.filter(_.contains(notifId))

        Try(pool.withClient(_.rpush(createKey(userId), elementDeleted: _*))) match {
          case -\/(e) =>
            Logger.error("Unable to set deleted in Redis")
            "Ko"
          case \/-(_) => "Ok"
        }

      case _ =>
        Logger.error("Unable to set deleted in Redis")
        "Ko"
    }

  }

  def retrieveInRedis(userId: UUID): String = {

    Try(pool.withClient(_.lrange(createKey(userId), 0, -1))) match {
      case \/-(notifs: List[String]) =>
        s"[${notifs.mkString(",")}]"
      case _ =>
        Logger.error("Unable to retreive in Redis")
        "Ko"
    }
  }


  override def postStop() =
    manager ! Quit(userId)
}


object NotificationCommandHandler {

  case class Command(command: String, parameter: Option[String])

  implicit val commandFormat = Json.format[Command]


  val COMMAND_SET_READ = "SET_READ"
  val COMMAND_LIST_ALL = "LIST_ALL"


  def props(userId: UUID, out: ActorRef, manager: ActorRef, pool: Pool) = Props(new NotificationCommandHandler(userId, out, manager, pool))
}