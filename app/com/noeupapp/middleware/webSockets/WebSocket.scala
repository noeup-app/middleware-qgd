package com.noeupapp.middleware.webSockets

import java.util.UUID

import play.api.mvc._
import play.api.Play.current
import akka.actor._
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.utils.BearerTokenGenerator
import com.noeupapp.middleware.webSockets.WebSocketAction._
import play.api.Logger
import play.api.i18n.MessagesApi

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent

import scala.collection.mutable
import scala.concurrent.duration._
import scalaz.{-\/, \/-}



object WebSocketAction {

  implicit val actorSystem = ActorSystem("websocketsystem")


  sealed trait WebSocketAction
  case class Broadcast(msg: String) extends WebSocketAction
  case class Send(userId: UUID, msg: String) extends WebSocketAction
  case class Join(userId: UUID, out: ActorRef) extends WebSocketAction
  case class Quit(userId: UUID) extends WebSocketAction

  case class UnknownClient(out: ActorRef)
}

class WebSocketTest @Inject()(val messagesApi: MessagesApi,
                              val env: Environment[Account, BearerTokenAuthenticator],
                              oAuthAccessTokenService: OAuthAccessTokenService)
  extends Silhouette[Account, BearerTokenAuthenticator] {

//  Logger.info(s"WebSocketTest - launched in 5 s")


  val webSocketManagerActor = actorSystem.actorOf(WebSocketManagerActor.props, "webSocketManagerActor")
//  val webSocketSecurityActor = actorSystem.actorOf(WebSocketSecurityActor.props, "webSocketSecurityActor")


  actorSystem.scheduler.schedule(2.seconds, 2.seconds) {
    webSocketManagerActor ! Broadcast(s"Salut les gens ${UUID.randomUUID()}")
  }

//  def socket = WebSocket.tryAcceptWithActor[String, String] { request =>
  def socket = WebSocket.acceptWithActor[String, String] { request => out =>

    Logger.info(s"WEB_SOCKET - $request")

    WebSocketSecurityActor.props(out, webSocketManagerActor, oAuthAccessTokenService)


//    implicit val req = Request(request, AnyContentAsEmpty)
//    SecuredRequestHandler { securedRequest =>
//      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
//    }.map {
//      case HandlerResult(r, Some(user)) => Right(out => MyWebSocketActor.props(user.user.id, out, webSocketManagerActor))
//      case HandlerResult(r, None) => Left(r)
//    }



//    request.headers.get("X-Auth-Token") match {
//      case None =>
//        Logger.error("X-Auth-Token is not found in request")
//        Future.successful(Left(Forbidden))
//      case Some(rawToken) => oAuthAccessTokenService.find(rawToken) map {
//        case \/-(token) if ! token.isExpired =>
//          token.userId match {
//            case None =>
//              Logger.error("token.userId is not defined")
//              Left(Forbidden)
//            case Some(userId) =>
//              Right(out => MyWebSocketActor.props(userId, out, webSocketManagerActor))
//          }
//        case \/-(token)  =>
//          Logger.error("token.userId is not defined")
//          Left(Forbidden)
//        case e @ -\/(_)  =>
//          Logger.error(s"Error occurred while retrieving token : $e")
//          Left(InternalServerError("Unable to check the token"))
//      }
//    }


  }

}


class WebSocketManagerActor extends Actor {

  val clients: mutable.Map[UUID, ActorRef] = mutable.Map.empty

  def receive = {
    case Join(userId, out) => clients += (userId -> out)
    case Quit(userId)      => clients -= userId
    case Send(userId, msg) => clients.get(userId).foreach(_ ! msg)
    case Broadcast(msg)    => clients.values.foreach(_ ! msg)
  }
}

object WebSocketManagerActor {
  def props = Props[WebSocketManagerActor]
}


class WebSocketSecurityActor (out: ActorRef, manager: ActorRef, oAuthAccessTokenService: OAuthAccessTokenService) extends Actor{

  var messageManagerActor: Option[ActorRef] = Option.empty

  val scheduler =
    actorSystem.scheduler.scheduleOnce(1.minute) {
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
                val props: Props = MyWebSocketActor.props(userIdFound, out, manager)
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


class MyWebSocketActor(userId: UUID, out: ActorRef, manager: ActorRef) extends Actor {

  def receive = {
    case msg: String =>
      Logger.info(s"WEB_SOCKET - $msg")
      out ! ("I received your message: " + msg)
  }

  override def postStop() =
    manager ! Quit(userId)

}

object MyWebSocketActor {
  def props(userId: UUID, out: ActorRef, manager: ActorRef) = Props(new MyWebSocketActor(userId, out, manager))
}