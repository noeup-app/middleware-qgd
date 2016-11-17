package com.noeupapp.middleware
package webSockets

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.utils.BearerTokenGenerator
import com.noeupapp.middleware.webSockets.WebSocketAction.Join
import play.api.Logger
import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import webSockets.WebSocketAction.actorSystem

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketSecurityActor (out: ActorRef, manager: ActorRef, oAuthAccessTokenService: OAuthAccessTokenService) extends Actor{

  var messageManagerActor: Option[ActorRef] = Option.empty

  val scheduler =
    actorSystem.scheduler.scheduleOnce(1 minute) {
      out ! "Error : token not sent"
      self ! PoisonPill
    }

  def checkToken(rawToken: String): Future[_] = {
    scheduler.cancel()
    BearerTokenGenerator.isToken(rawToken) match {
      case true =>
        oAuthAccessTokenService.find(rawToken) map {
          case \/-(token) if ! token.isExpired =>
            token.userId match {
              case None =>
                Logger.error("token.userId is not defined")
                out ! "Forbidden"
                self ! PoisonPill
              case Some(userIdFound) =>
                val props: Props = PingPongActor.props(userIdFound, out, manager)
                messageManagerActor = Some(actorSystem.actorOf(props, "webSocketSecurityActor"))
                manager ! Join(userIdFound, out)
            }
          case \/-(token)  =>
            Logger.info(s"WS expired token")
            out ! "Forbidden"
            self ! PoisonPill
          case e @ -\/(_)  =>
            Logger.error(s"WS error $e")
            out ! "Forbidden"
            self ! PoisonPill
        }
      case false =>
        Logger.error(s"BearerTokenGenerator.isToken($rawToken) -> false")
        out ! "Forbidden"
        Future.successful(self ! PoisonPill)
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
  def props(out: ActorRef, manager: ActorRef, oAuthAccessTokenService: OAuthAccessTokenService) = Props(new WebSocketSecurityActor(out, manager, oAuthAccessTokenService))
}

