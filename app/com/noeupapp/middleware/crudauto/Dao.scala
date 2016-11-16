package com.noeupapp.middleware.crudauto

import javax.inject.Inject

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.db.slick.DatabaseConfigProvider
import slick.backend.DatabaseConfig
import slick.dbio.{Effect, NoStream}
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend
import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.profile.FixedSqlAction

import scala.concurrent.Future
import scala.language.higherKinds
import scalaz.{-\/, \/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 15/11/2016.
  */
class Dao @Inject()(dbConfigProvider: DatabaseConfigProvider) {

  lazy val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  lazy val db: JdbcBackend#DatabaseDef           = dbConfig.db


  def run[R, S <: NoStream, F <: Effect](query: FixedSqlAction[R, S, F]): Future[Expect[R]] = {
    db.run(query)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runForAll[U <: Entity, V <: Table[U], C[_]](query: Query[V, U, C]): Future[Expect[C[U]]] = {
    db.run(query.result)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runForHeadOption[U <: Entity, V <: Table[U], C[_]](query: Query[V, U, C]): Future[Expect[Option[U]]] = {
    db.run(query.result.headOption)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }


}
