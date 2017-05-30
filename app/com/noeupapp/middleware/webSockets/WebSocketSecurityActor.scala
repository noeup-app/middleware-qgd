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
import webSockets.WebSocketAction.actorSystem

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketSecurityActor (out: ActorRef, manager: ActorRef, userService: UserService) extends Actor{

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
        val props: Props = PingPongActor.props(user.id, out, manager)
        messageManagerActor = Some(actorSystem.actorOf(props, "webSocketSecurityActor"))
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
  def props(out: ActorRef, manager: ActorRef, userService: UserService) = Props(new WebSocketSecurityActor(out, manager, userService))
}

