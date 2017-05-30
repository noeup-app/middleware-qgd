package com.noeupapp.middleware.notifications

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.BroadcastGroup
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.webSockets.WebSocketAction
import com.noeupapp.middleware.Global._
import com.noeupapp.middleware.notifications.notifiers.WebSocketNotificationActor
import play.api.Logger

/**
  * Created by damien on 30/05/2017.
  */
class Notification {

  import NotificationActor._

  def registerListener(actor: ActorRef) = notificationActor ! RegisterListener(actor)

  def send(user: User, messageType: String, messageData: String) =
    notificationActor ! Send(user, messageType, messageData)

}


class NotificationActor extends Actor {

  import NotificationActor._

  private var actors: List[String] = List.empty

  private var observersMap: Map[String, ActorRef] = Map.empty

  override def receive: Receive = {
    case RegisterListener(actor) => registerListener(actor)
    case Send(user, messageType, messageData) => send(user, messageType, messageData)
  }

  private def registerListener(actor: ActorRef) = { actors = actors ++ List(actor.path.toString) }

  private def send(user: User, messageType: String, messageData: String) = {

    if(actors.isEmpty){
      // TODO send a mail ?
      Logger.warn("No listener registered !")
    }

    val observersName = s"observers-${actors.mkString("-").hashCode}"

    val listeners =
      observersMap.get(observersName) match {
        case Some(actor) => actor
        case None =>
          val observers: ActorRef = context.actorOf(BroadcastGroup(actors).props(), observersName)
          observersMap = observersMap + (observersName -> observers)
          observers
      }

    listeners ! NotificationMessage(user, messageType, messageData)
  }

}

object NotificationActor {
  case class RegisterListener(actor: ActorRef)
  case class Send(user: User, messageType: String, messageData: String)


  def props = Props[NotificationActor]

  val notificationActor: ActorRef = actorSystem.actorOf(NotificationActor.props, "notification-actor")

}