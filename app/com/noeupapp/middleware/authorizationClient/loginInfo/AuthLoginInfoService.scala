package com.noeupapp.middleware.authorizationClient.loginInfo

import java.sql.Connection

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


}
