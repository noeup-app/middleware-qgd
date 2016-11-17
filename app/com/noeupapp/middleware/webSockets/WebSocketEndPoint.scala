package com.noeupapp.middleware
package webSockets

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




class WebSocketEndPoint @Inject()(val messagesApi: MessagesApi,
                                  val env: Environment[Account, BearerTokenAuthenticator],
                                  oAuthAccessTokenService: OAuthAccessTokenService)
  extends Silhouette[Account, BearerTokenAuthenticator] {



//  actorSystem.scheduler.schedule(2.seconds, 2.seconds) {
//    webSocketManagerActor ! Broadcast(s"Salut les gens ${UUID.randomUUID()}")
//  }

//  def socket = WebSocket.tryAcceptWithActor[String, String] { request =>
  def socket = WebSocket.acceptWithActor[String, String] { request => out =>

    Logger.info(s"WEB_SOCKET - $request")

    WebSocketSecurityActor.props(out, webSocketManagerActor, oAuthAccessTokenService)

  }

}


