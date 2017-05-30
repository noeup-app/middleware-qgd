package com.noeupapp.middleware.webSockets

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import com.noeupapp.middleware.notifications.NotificationMessage
import com.noeupapp.middleware.webSockets.WebSocketAction.{Broadcast, Clients, Join, Quit, Send}
import play.api.Logger

import scala.collection.mutable

class WebSocketManagerActor extends Actor {

  Logger.debug("WebSocketManagerActor created")

  // Actor is executed in only on thread so its thread safe
  val clients: mutable.Map[UUID, ActorRef] = mutable.Map.empty

  def receive = {
    case Join(userId, out) => clients += (userId -> out)
    case Quit(userId)      => clients -= userId
    case Send(userId, msg) => clients.get(userId).foreach(_ ! msg)
    case Broadcast(msg)    => clients.values.foreach(_ ! msg)
    case Clients           => sender ! clients.keys.toList
    case e => Logger.error(s"WebSocketManagerActor $e")
  }
}

object WebSocketManagerActor {
  def props = Props[WebSocketManagerActor]
}