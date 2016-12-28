package com.noeupapp.middleware.authorizationServer.client


import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError._

import scala.concurrent.Future
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scalaz.\/-

/**
  * Created by damien on 25/07/2016.
  */
class ClientService @Inject() (clientDAO: ClientDAO){


  def findAll(): Future[Expect[List[Client]]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.findAll())
    }


  def findByClientId(clientId: String): Future[Expect[Option[Client]]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.findByClientId(clientId))
    }


  def findByClientIDAndClientSecret(clientId: String, clientSecret: String): Future[Expect[Option[Client]]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.findByClientIDAndClientSecret(clientId, clientSecret))
    }


  def findByAccessToken(accessToken: String): Future[Expect[Option[Client]]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.findByAccessToken(accessToken))
    }


  def insert(client: Client): Future[Expect[Boolean]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.insert(client))
    }


  def update(client: Client): Future[Expect[Boolean]] =
    TryBDCall { implicit c =>
      \/-(clientDAO.update(client))
    }


  def delete(clientId: String): Future[Expect[Boolean]] =
  TryBDCall { implicit c =>
      \/-(clientDAO.delete(clientId))
    }

}
