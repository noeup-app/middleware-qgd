package com.noeupapp.middleware.crudauto.model


import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Created by damien on 24/11/2016.
  */
case class AuthTest(id: Long, name: String) extends Entity[Long] {
  def this(l: AuthTestIn) =
    this(0, l.name)

  override def withNewId(id: Long): Entity[Long] = copy(id = id)
}

case class AuthTestOut(id: Long, name: String) extends Entity[Long] {
  override def withNewId(id: Long): Entity[Long] = copy(id = id)
}

/**
  * Required parameters to add or update an new limitation
  */
case class AuthTestIn(name: String, test: Long)

object AuthTest extends GlobalReadsWrites {

  implicit val AuthTestFormat = Json.format[AuthTest]
  implicit val AuthTestInFormat = Json.format[AuthTestIn]
  implicit val AuthTestOutFormat = Json.format[AuthTestOut]


  implicit def toAuthTestOut(p: AuthTest): AuthTestOut =
    AuthTestOut(p.id, p.name)

  val tableName = "authtest"


  val tq = TableQuery[AuthTestTableDef]


//  tq.baseTableRow.testFk.


  def createTable(db: JdbcBackend#DatabaseDef): Future[Unit] = {
    db.run(DBIO.seq(tq.delete))
      .recover{
        case _: Exception =>
          db.run(DBIO.seq(tq.schema.create))
      }
  }

  def dropTable(db: JdbcBackend#DatabaseDef) = db.run(DBIO.seq(tq.schema.drop))

  def all(db: JdbcBackend#DatabaseDef, id: Long) =
    db.run(
      tq.result
    )
}


class AuthTestTableDef(tag: Tag) extends Table[AuthTest](tag, "authtest") with PKTable {
  def id = column[Long]("id")
  def name = column[String]("name")

  override def * = (id, name) <> ((AuthTest.apply _).tupled, AuthTest.unapply)

  def pk = primaryKey("authtest_pk", id)

}
