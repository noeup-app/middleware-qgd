package com.noeupapp.middleware.authorizationClient.loginInfo

import java.sql.Connection
import java.util.UUID

import anorm.SQL
import com.google.inject.Inject
import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.Future
import scala.language.postfixOps
import scalaz.\/-


/**
  * Created by damien on 23/02/2017.
  */
class AuthLoginInfoService @Inject() (authLoginInfoDao: AuthLoginInfoDao){


  def find(providerId: String, providerKey: String): Future[Expect[Option[AuthLoginInfo]]] =
    TryBDCall{ implicit connection =>
      \/-(authLoginInfoDao.find(providerId, providerKey))
    }


  def find(loginInfo: api.LoginInfo): Future[Expect[Option[AuthLoginInfo]]] =
    find(loginInfo.providerID, loginInfo.providerKey)


  def add(authLoginInfo: AuthLoginInfo): Future[Expect[AuthLoginInfo]] =
    TryBDCall{ implicit connection =>
      authLoginInfoDao.add(authLoginInfo)
      \/-(authLoginInfo)
    }

  def update(providerId: String, providerKey: String, authLoginInfo: AuthLoginInfo): Future[Expect[AuthLoginInfo]] =
    TryBDCall{ implicit connection =>
      authLoginInfoDao.update(providerId, providerKey, authLoginInfo)
      \/-(authLoginInfo)
    }

  def update(loginInfo: api.LoginInfo, authLoginInfo: AuthLoginInfo): Future[Expect[AuthLoginInfo]] =
    this.update(loginInfo.providerID, loginInfo.providerKey, authLoginInfo)

  def delete(userId: UUID): Future[Expect[Unit]] =
    TryBDCall{ implicit connection =>
      authLoginInfoDao.delete(userId)
      \/-(())
    }

}
