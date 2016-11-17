package com.noeupapp.middleware.webSockets

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.webSockets.WebSocketAction.Quit
import play.api.Logger


class PingPongActor(userId: UUID, out: ActorRef, manager: ActorRef) extends Actor {

  def receive = {
    case msg: String =>
      Logger.info(s"WEB_SOCKET - $msg")
      out ! ("I received your message: " + msg)
  }

  override def postStop() =
    manager ! Quit(userId)

}

object PingPongActor {
  def props(userId: UUID, out: ActorRef, manager: ActorRef) = Props(new PingPongActor(userId, out, manager))
}