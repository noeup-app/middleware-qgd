package com.noeupapp.middleware.crudauto.model

import java.util.UUID

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Created by damien on 24/11/2016.
  */
case class Thing(id: UUID,
                 name: String,
                 test: UUID
                ) extends Entity {
  def this(l: ThingIn) =
    this(UUID.randomUUID(), l.name, l.test)
}

case class ThingOut(id: UUID,
                    name: String,
                    test: UUID
                   ) extends Entity

/**
  * Required parameters to add or update an new limitation
  */
case class ThingIn(name: String,
                   test: UUID) extends Entity

object Thing extends GlobalReadsWrites {

  implicit val ThingFormat = Json.format[Thing]
  implicit val ThingInFormat = Json.format[ThingIn]
  implicit val ThingOutFormat = Json.format[ThingOut]


  implicit def toThingOut(p: Thing): ThingOut =
    ThingOut(p.id, p.name, p.test)

  val tableName = "thing"


  val tq = TableQuery[ThingTableDef]


//  tq.baseTableRow.testFk.


  def createTable(db: JdbcBackend#DatabaseDef): Future[Unit] = {
    db.run(DBIO.seq(tq.delete))
      .recover{
        case _: Exception =>
          db.run(DBIO.seq(tq.schema.create))
      }
  }

  def dropTable(db: JdbcBackend#DatabaseDef) = db.run(DBIO.seq(tq.schema.drop))

  def all(db: JdbcBackend#DatabaseDef, id: UUID) =
    db.run(
      tq.result
    )
}


class ThingTableDef(tag: Tag) extends Table[Thing](tag, "thing") with PKTable {
  def id = column[UUID]("id")
  def name = column[String]("name")
  def test = column[UUID]("test")

  override def * = (id, name, test) <> ((Thing.apply _).tupled, Thing.unapply)

  // A reified foreign key relation that can be navigated to create a join

  def pk = primaryKey("thing_pk", id)

  def testFk = foreignKey("tests_fkey", test, Test.tq)(_.id)

}
