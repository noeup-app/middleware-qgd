package com.noeupapp.middleware.authorizationServer.authCode

import java.sql.Timestamp
import java.util.{Date, UUID}

import com.google.inject.Inject
import com.noeupapp.middleware.authorizationServer.client.ClientDAO
import com.noeupapp.middleware.utils.AuthCodeGenerator
import play.api.db.DB
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime

import scalaz.\/-
class AuthCodeService @Inject() (authCodeDAO: AuthCodeDAO,
                                 clientDAO: ClientDAO) {


  /**
    * Generate a new AuthCode for given client and other details.
    *
    * @param clientId
    * @param redirectUri
    * @param scope
    * @param userId
    * @param expiresIn
    * @return
    */
  def generateAuthCodeForClient(clientId: String, redirectUri: String, scope: String, userId: UUID, expiresIn: Int): Future[Option[AuthCode]] = Future {

    DB.withTransaction({ implicit c =>
      clientDAO.findByClientId(clientId).flatMap {
        client =>
          val code = AuthCodeGenerator.generateAuthCode()
          val createdAt = DateTime.now()
          val authCode = AuthCode(
            code,
            createdAt,
            clientId,
//            Some(scope),
            expiresIn.toLong,
            Some(redirectUri),
            userId,
            used = false)

          // replace with new auth code
          for{
            _ <- Try(authCodeDAO.setAuthCodeAsUsed(authCode.authorizationCode)).toOption
            _ <- Try(authCodeDAO.insert(authCode)).toOption
          } yield authCode
      }
    })
  }


  def find(code: String): Future[Expect[Option[AuthCode]]] =
    TryBDCall[Option[AuthCode]]{ implicit c =>
        \/-(authCodeDAO.find(code))
      }
   // }.toOption.flatten

}
