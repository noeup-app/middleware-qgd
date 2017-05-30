package com.noeupapp.middleware
package webSockets


import java.util.UUID

import play.api.mvc._
import play.api.Play.current
import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.webSockets.WebSocketAction._
import org.sedis.Pool
import play.api.Logger
import play.api.i18n.MessagesApi





class WebSocketEndPoint @Inject()(val messagesApi: MessagesApi,
                                  val env: Environment[Account, BearerTokenAuthenticator],
                                  userService: UserService,
                                 pool: Pool)
  extends Silhouette[Account, BearerTokenAuthenticator] {



//  actorSystem.scheduler.schedule(2.seconds, 2.seconds) {
//    webSocketManagerActor ! Broadcast(s"Salut les gens ${UUID.randomUUID()}")
//  }

//  def socket = WebSocket.tryAcceptWithActor[String, String] { request =>
  def socket = WebSocket.acceptWithActor[String, String] { request => out =>

    Logger.info(s"WEB_SOCKET - $request")

    WebSocketSecurityActor.props(out, webSocketManagerActor, userService, pool)

  }


  def test(msg: String) = Action {
    webSocketManagerActor !
      Send(UUID.fromString("49ca594b-7f48-4616-879f-87050b7c730b"),
        WebSocketMessage[String]("test", msg))
    Ok("")
  }

}


