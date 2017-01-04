package com.noeupapp.middleware.crudauto

import javax.inject.Inject

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsError, JsResult, JsSuccess}
import slick.backend.DatabaseConfig
import slick.dbio.{Effect, NoStream}
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend
import slick.driver._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.profile.{BasicAction, SqlStreamingAction}

import scala.concurrent.Future
import scala.language.higherKinds
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._


/**
  * Created by damien on 15/11/2016.
  */
class Dao @Inject()(dbConfigProvider: DatabaseConfigProvider) {

  lazy val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]
  lazy val db: JdbcBackend#DatabaseDef           = dbConfig.db


  def run[R, S <: NoStream, F <: Effect](query: BasicAction[R, S, F]): Future[Expect[R]] = {
    db.run(query)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runSqlStreamingAction[R, S <: NoStream, F <: Effect](query: SqlStreamingAction[R, S, F]): Future[Expect[R]] = {
    db.run(query)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runTransformer[R, T, S, F <: Effect](query: SqlStreamingAction[R, S, F])(transformer: (R) => JsResult[T]): Future[Expect[T]] = {
    db.run(query)
      .map(transformer)
      .map{
        case JsSuccess(value, _) => \/-(value)
        case JsError(errors) => -\/(FailError(s"Unable to validate json, errors : ${errors.mkString(", ")}"))
      }
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runForAll[U <: Entity[Any], V <: Table[U], C[_]](query: Query[V, U, C]): Future[Expect[C[U]]] = {
    db.run(query.result)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runCount[U <: Entity[_], V <: Table[U], C[_]](query: Query[V, U, C]): Future[Expect[Int]] = {
    db.run(query.length.result)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }

  def runForHeadOption[U <: Entity[_], V <: Table[U], C[_]](query: Query[V, U, C]): Future[Expect[Option[U]]] = {
    db.run(query.result.headOption)
      .map(\/-(_))
      .recover{
        case e: Exception => -\/(FailError(e))
      }
  }


}
