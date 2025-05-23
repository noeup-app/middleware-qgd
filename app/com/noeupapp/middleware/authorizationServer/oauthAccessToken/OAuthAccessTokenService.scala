package com.noeupapp.middleware.authorizationServer.oauthAccessToken

import java.util.{Date, UUID}

import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.errorHandle.{ExceptionEither, FailError}
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._



class OAuthAccessTokenService @Inject() (oAuthAccessTokenDAO: OAuthAccessTokenDAO,
                                         userService: UserService) {

  val logger = Logger(this.getClass)


  /**
    * Fetch AccessToken by its ID.
    * @param token String
    * @return
    */
  def find(token: String): Future[Expect[OAuthAccessToken]] = {
    Logger.trace(s"OAuthAccessTokenService.find($token)")
    ExceptionEither.TryBDCall { implicit c =>
      oAuthAccessTokenDAO.find(token) match {
        case Some(r) => \/-(r)
        case None => -\/(FailError("Access token not found"))
      }
    }
  }

  /**
    * Find AccessToken by User and Client
    * @param userId
    * @param clientId
    * @return
    */
  def findByUserAndClient(userId: UUID, clientId: String): Future[Expect[OAuthAccessToken]] = {
    Logger.trace(s"OAuthAccessTokenService.findByUserAndClient($userId, $clientId)")
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.findByUserAndClient(userId, clientId) match {
        case Some(r) => \/-(r)
        case None => -\/(FailError("Access token not found"))
      }
    }
  }

  /**
    * Find Refresh Token by its value
    * @param refreshToken
    * @return
    */
  def findByRefreshToken(refreshToken: String): Future[Expect[OAuthAccessToken]] = {
    Logger.trace(s"OAuthAccessTokenService.findByRefreshToken($refreshToken)")
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.findByRefreshToken(refreshToken) match {
        case Some(r) => \/-(r)
        case None => -\/(FailError("Access token not found"))
      }
    }
  }


  /**
    * Add a new AccessToken
    * @param accessToken
    * @return
    */
  def insert(accessToken: OAuthAccessToken): Future[Expect[Boolean]] = {
    Logger.trace(s"OAuthAccessTokenService.insert($accessToken)")
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.insert(accessToken)
      \/-(true)
    }
  }

  /**
    * Update existing AccessToken associated with a user and a client.
    * @param accessToken
    * @param userId
    * @param clientId
    * @return
    */
  def updateByUserAndClient(accessToken: OAuthAccessToken, userId: Int, clientId: String) = ???
  // session.withTransaction {
  //   accessTokens.where(a => a.clientId === clientId && a.userId === userId).delete
  //   accessTokens.insert(accessToken)
  // }


  /**
    * Update AccessToken object based for the ID in accessToken object
    *
    * @param accessToken
    * @return
    */
  def update(accessToken: OAuthAccessToken) = ???
  //accessTokens.where(_.id === accessToken.id).update(accessToken)


  def deleteExistingAndCreate(tokenObject: OAuthAccessToken, user_uuid: UUID, client_id: String): Future[Expect[OAuthAccessToken]] = {
    for {
      _ <- EitherT(deleteByClientIdAndUserId(client_id, user_uuid))
      _ <- EitherT(insert(tokenObject))
    } yield tokenObject
  }.run


  def deleteByClientIdAndUserId(client_id: String, user_uuid: UUID): Future[Expect[Boolean]] = {
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.deleteByClientIdAndUserId(client_id, user_uuid)
      \/-(true)
    }
  }

  def deleteByRefreshToken(refreshToken: String): Future[Expect[Boolean]] = {
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.deleteByRefreshToken(refreshToken)
      \/-(true)
    }
  }

  def deleteByAccessToken(accessToken: String): Future[Expect[Boolean]] = {
    ExceptionEither.TryBDCall{ implicit c =>
      oAuthAccessTokenDAO.deleteByAccessToken(accessToken)
      \/-(true)
    }
  }


  def refreshAccessToken(refreshToken: String, newToken: String, createdAt: Date, expiresIn: Long): Future[Expect[Boolean]] = {
    for {
      accessToken <- EitherT(findByRefreshToken(refreshToken))
      _           <- EitherT(deleteByRefreshToken(refreshToken))
      _           <- EitherT(insert(accessToken.copy( refreshToken = Some(refreshToken),
                                                      accessToken = newToken,
                                                      createdAt = createdAt,
                                                      expiresIn = Some(expiresIn)
                                                    )))
    } yield true
  }.run


}
