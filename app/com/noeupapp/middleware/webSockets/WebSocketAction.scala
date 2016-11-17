package com.noeupapp.middleware.webSockets

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}

/**
  * Created by damien on 17/11/2016.
  */
object WebSocketAction {

  implicit val actorSystem = ActorSystem("websocketsystem")


  sealed trait WebSocketAction
  case class Broadcast(msg: String) extends WebSocketAction
  case class Send(userId: UUID, msg: String) extends WebSocketAction
  case class Join(userId: UUID, out: ActorRef) extends WebSocketAction
  case class Quit(userId: UUID) extends WebSocketAction
  case object Clients extends WebSocketAction

  case class UnknownClient(out: ActorRef)
}
