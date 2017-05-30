package com.noeupapp.middleware
package webSockets

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.utils.BearerTokenGenerator
import com.noeupapp.middleware.webSockets.WebSocketAction.Join
import play.api.Logger
import akka.actor._
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticatorDAO
import com.noeupapp.middleware.authorizationServer.oauth2.AuthorizationHandler
import com.noeupapp.middleware.entities.user.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import com.noeupapp.middleware.Global._
import com.noeupapp.middleware.notifications.NotificationCommandHandler
import org.sedis.Pool

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketSecurityActor (out: ActorRef, manager: ActorRef, userService: UserService, pool: Pool) extends Actor{

  val forbidden = WebSocketMessage[String]("Forbidden", "Forbidden")
  val tokenNotFound = WebSocketMessage[String]("Unauthorized", "The token is not found")


  var messageManagerActor: Option[ActorRef] = Option.empty

  val scheduler =
    actorSystem.scheduler.scheduleOnce(1 minute) {
      out ! tokenNotFound
      self ! PoisonPill
    }

  def checkToken(rawToken: String): Future[_] = {
    scheduler.cancel()

    if(! BearerTokenGenerator.isToken(rawToken)) {
      Logger.error(s"BearerTokenGenerator.isToken($rawToken) -> false")
      out ! forbidden
      return Future.successful(self ! PoisonPill)
    }

    userService.findUserByToken(rawToken) map {
      case \/-(Some(user)) =>
        // Todo decoupler : NotificationCommandHandler
        val props: Props = NotificationCommandHandler.props(user.id, out, manager, pool)
        messageManagerActor = Some(actorSystem.actorOf(props, s"notificationCommandHandler-${user.id}"))
        manager ! Join(user.id, out)

      case \/-(None) =>
        Logger.error("token.userId is not defined")
        out ! forbidden
        self ! PoisonPill

      case e @ -\/(_)  =>
        Logger.error(s"WS error $e")
        out ! forbidden
        self ! PoisonPill

    }
  }


  def receive = {
    case msg: String =>
      Logger.info(s"[${hashCode()}]WebSocketSecurityActor : $msg")
      scheduler.isCancelled match {
        case true => messageManagerActor.foreach(_ ! msg)
        case false =>
          checkToken(msg) map { _ =>
            messageManagerActor.foreach(_ ! msg)
          }
      }
  }

  override def postStop() =
    messageManagerActor.foreach(_ ! PoisonPill)


}

object WebSocketSecurityActor {
  def props(out: ActorRef, manager: ActorRef, userService: UserService, pool: Pool) = Props(new WebSocketSecurityActor(out, manager, userService, pool))
}

