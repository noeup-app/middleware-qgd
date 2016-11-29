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
case class Test( id: UUID,
                 name: String,
                 typeL: String,
                 priority: Int
               ) extends Entity {
  def this(l: TestIn) =
    this(UUID.randomUUID(), l.name, l.typeL, l.priority)
}
case class TestOut( id: UUID,
                    name: String,
                    typeL: String,
                    priority: Int) extends Entity

/**
  * Required parameters to add or update an new limitation
  */
case class TestIn( name: String,
                   typeL: String,
                   priority: Int) extends Entity

object Test extends GlobalReadsWrites {

  implicit val TestFormat = Json.format[Test]
  implicit val TestInFormat = Json.format[TestIn]
  implicit val TestOutFormat = Json.format[TestOut]


  implicit def toTestOut(p:Test):TestOut =
    TestOut(p.id, p.name, p.typeL, p.priority)

  val tableName = "test"


  val tq = TableQuery[TestTableDef]



  def createTable(db: JdbcBackend#DatabaseDef): Future[Unit] = {
    db.run(DBIO.seq(tq.delete))
      .recover{
        case _: Exception =>
          db.run(DBIO.seq(tq.schema.create))
      }
  }

  def dropTable(db: JdbcBackend#DatabaseDef) = db.run(DBIO.seq(tq.schema.drop))

  def populate(db: JdbcBackend#DatabaseDef, id: UUID) = {
    val id1 = id
    val id2 = UUID.randomUUID()
    db.run(
      tq ++= Seq(
        Test(id1, "my test 1", "super type", 5),
        Test(id2, "my test 2", "type", 1234)
      )
    )
    db.run(
      Thing.tq ++= Seq(
        Thing(UUID.randomUUID(), "thing 1", id1),
        Thing(UUID.randomUUID(), "thing 2", id1)
      )
    )
  }

  def all(db: JdbcBackend#DatabaseDef, id: UUID) =
    db.run(
      tq.result
    )
}



class TestTableDef(tag: Tag) extends Table[Test](tag, "test") with PKTable[UUID] {
  def id             = column[UUID]("id")
  def name           = column[String]("name")
  def typeL          = column[String]("type")
  def priority       = column[Int]("priority")
  override def *     = (id, name, typeL, priority) <> ((Test.apply _).tupled, Test.unapply)
  // A reified foreign key relation that can be navigated to create a join

  def pk = primaryKey("test_pk", id)


}
