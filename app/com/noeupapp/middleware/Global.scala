package com.noeupapp.middleware

import akka.actor.ActorSystem

/**
  * Created by damien on 30/05/2017.
  */
object Global {
  implicit val actorSystem = ActorSystem("app-actor-sys")
}
