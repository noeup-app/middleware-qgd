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
import org.sedis.Pool

import scala.concurrent.duration._
import scala.language.postfixOps

class WebSocketSecurityActor (out: ActorRef, manager: ActorRef, userService: UserService, pool: Pool) extends Actor{

  val forbidden: String = WebSocketMessage[String]("error", "Forbidden")
  val tokenNotFound: String = WebSocketMessage[String]("error", "The token is not found")

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
      if (! scheduler.isCancelled) {
        checkToken(msg)
      }
  }

}

object WebSocketSecurityActor {
  def props(out: ActorRef, manager: ActorRef, userService: UserService, pool: Pool) = Props(new WebSocketSecurityActor(out, manager, userService, pool))
}

