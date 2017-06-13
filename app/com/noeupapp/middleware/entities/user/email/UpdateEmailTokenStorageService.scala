package com.noeupapp.middleware.entities.user.email

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.ExceptionEither.FTry
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.BearerTokenGenerator
import org.sedis.Pool

import scala.concurrent.Future

/**
  * Created by damien on 13/06/2017.
  */
class UpdateEmailTokenStorageService @Inject()(pool: Pool){

  def createAndSaveToken(idUser: UUID, newEmail: String): Future[Expect[String]] =
    FTry{
      val token = BearerTokenGenerator.generateToken(40)
      val key = UserEmailTokenKey(token)
      pool.withClient(_.set(key, UserEmailTokenValue(idUser, newEmail)))
      pool.withClient(_.expire(key, 60 * 60 * 24))
      token
    }


  def retrieveToken(token: String): Future[Expect[Option[UserEmailTokenValue]]] =
    FTry{
      pool.withClient(_.get(UserEmailTokenKey(token)).flatMap(v => v))
    }


  def deleteToken(token: String): Future[Expect[Unit]] =
    FTry{
      pool.withClient(_.del(UserEmailTokenKey(token)))
    }

}
