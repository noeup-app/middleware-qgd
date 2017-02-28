package com.noeupapp.middleware.packages.event

import java.util.UUID

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.packages.pack.Pack
import play.api.libs.json.{JsValue, Json}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import com.noeupapp.middleware.utils.GlobalReadsWrites
import org.joda.time.DateTime

import scala.language.{implicitConversions, postfixOps}



case class Event(id: UUID,
                 actionName: String,
                 triggered: DateTime,
                 userId: UUID,
                 packageId: Long,
                 params: Option[JsValue]
                ) extends Entity[UUID] {

  override def withNewId(id: UUID): Entity[UUID] = copy(id = id)

  def this(e: EventIn) = this(UUID.randomUUID(), e.actionName, DateTime.now(), e.userId, e.packageId, e.params)

}


case class EventIn(actionName: String,
                   userId: UUID,
                   packageId: Long,
                   params: Option[JsValue])

case class EventOut(id: UUID,
                   actionName: String,
                   triggered: DateTime,
                   userId: UUID,
                   packageId: Long,
                   params: Option[JsValue])

object Event extends GlobalReadsWrites {

  implicit val EventFormat    = Json.format[Event]
  implicit val EventInFormat  = Json.format[EventIn]
  implicit val EventOutFormat = Json.format[EventOut]

  val event = TableQuery[EventTableDef]

  implicit def toEventOut(e: Event): EventOut = EventOut(e.id, e.actionName, e.triggered, e.userId, e.packageId, e.params)

}


class EventTableDef(tag: Tag) extends Table[Event](tag, "package_events") with PKTable {

  def id = column[UUID]("id")

  def actionName = column[String]("action_name")

  def triggered = column[DateTime]("triggered")

  def userId = column[UUID]("user_id")

  def packageId = column[Long]("package_id")

  def params = column[Option[JsValue]]("params")

  override def * = (id, actionName, triggered, userId, packageId, params) <> ((Event.apply _).tupled, Event.unapply)

  def pk = primaryKey("package_events_pkey", id)

  def packageFkey = foreignKey("package_events_package_packages_id_fk", packageId, Pack.pack)(_.id)

}