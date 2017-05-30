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
      case Command(COMMAND_SET_READ, Some(notifId)) => Future.successful(\/-("OK"))
      case error => Future.successful(\/-(returnError(error)))
    }
  }

  def returnError(command: Command): String =
    Json.stringify(Json.obj("message_type" -> "error", "message_data" -> s"Command ($command) not found"))

  def returnError(command: String): String =
    Json.stringify(Json.obj("message_type" -> "error", "message_data" -> s"Command ($command) not found"))


  def retrieveInRedis(userId: UUID, notifId: UUID) = {

  }


  override def postStop() =
    manager ! Quit(userId)
}


object NotificationCommandHandler {

  case class Command(command: String, parameter: Option[String])

  implicit val commandFormat = Json.format[Command]


  val COMMAND_SET_READ = "SET_READ"


  def props(userId: UUID, out: ActorRef, manager: ActorRef, pool: Pool) = Props(new NotificationCommandHandler(userId, out, manager, pool))
}