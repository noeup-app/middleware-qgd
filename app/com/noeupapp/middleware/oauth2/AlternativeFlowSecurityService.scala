package com.noeupapp.middleware.oauth2


import com.google.inject.Inject
import com.noeupapp.middleware.authorizationServer.client.{Client, ClientService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}

/**
  * Created by damien on 25/07/2016.
  */
class AlternativeFlowSecurityService @Inject()(clientService: ClientService) {


  def checkIfClientIsAllowed(request: RequestHeader): Future[Expect[Client]] = {
    request.headers.get("X-Auth-Key") match {

      case Some(token) =>
        token.split(":").toList match {
          case clientId :: clientSecret :: Nil =>
            clientService.findByClientIDAndClientSecret(clientId, clientSecret) map {
              case \/-(Some(client)) => \/-(client)
              case \/-(None)         => -\/(FailError("Client is not found", errorType = NotFound))
              case -\/(e)            => -\/(e)
            }
          case _ => Future.successful(-\/(FailError("X-Auth-Key value format is not as expected", errorType = BadRequest)))
        }

      case None        => Future.successful(-\/(FailError("X-Auth-Key is not found", errorType = BadRequest)))
    }
  }



}
