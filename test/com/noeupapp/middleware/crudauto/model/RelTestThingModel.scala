package com.noeupapp.middleware.crudauto.model

import java.util.UUID

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.jdbc.JdbcBackend
import slick.lifted.{Index, PrimaryKey}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

case class RelTestThing( id: Long, test: UUID, thing: UUID) extends Entity[Long] {
  def this(l: RelTestThingIn) = this(0, l.test, l.thing)

  override def withNewId(id: Long): Entity[Long] = copy(id = id)
}
case class RelTestThingOut( id: Long, test: UUID, thing: UUID) extends Entity[Long] {
  override def withNewId(id: Long): Entity[Long] = copy(id = id)
}
case class RelTestThingIn( test: UUID, thing: UUID)

object RelTestThing extends GlobalReadsWrites {

  implicit val RelTestThingFormat = Json.format[RelTestThing]
  implicit val RelTestThingInFormat = Json.format[RelTestThingIn]
  implicit val RelTestThingOutFormat = Json.format[RelTestThingOut]


  implicit def toRelTestThingOut(p:RelTestThing):RelTestThingOut =
    RelTestThingOut(p.id, p.test, p.thing)

  val tableName = "rel_test_thing"

  val tq = TableQuery[RelTestThingTableDef]

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



class RelTestThingTableDef(tag: Tag) extends Table[RelTestThing](tag, RelTestThing.tableName) with PKTable {
  def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def test        = column[UUID]("test")
  def thing       = column[UUID]("thing")
  override def *  = (id, test, thing) <> ((RelTestThing.apply _).tupled, RelTestThing.unapply)
  // A reified foreign key relation that can be navigated to create a join

  def pk: PrimaryKey = primaryKey("rtt_pk", id)
  def unique: Index = index("rtt_unik", (test,thing), unique=true)

}
