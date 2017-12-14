package com.noeupapp.middleware.entities.relationEntityPackage

import java.util.UUID

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.utils.GlobalReadsWrites
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

import scala.language.{implicitConversions, postfixOps}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._


case class RelationEntityPackage(id: UUID,
                                 packageId: Long,
                                 entityId: UUID,
                                 billed: Option[DateTime],
                                 created: DateTime,
                                 optionState: Option[JsValue]) extends Entity[UUID] {

def this (e: RelationEntityPackageIn) =
  this(UUID.randomUUID(), e.packageId, e.entityId, e.billed, DateTime.now(), e.optionState)

override def withNewId (id: UUID): Entity[UUID] = copy(id = id)

}


case class RelationEntityPackageIn(packageId: Long,
                                   entityId: UUID,
                                   billed: Option[DateTime],
                                   optionState: Option[JsValue])


case class RelationEntityPackageOut(id: UUID,
                                    packageId: Long,
                                    entityId: UUID,
                                    billed: Option[DateTime],
                                    created: DateTime,
                                    optionState: Option[JsValue])

object RelationEntityPackage extends GlobalReadsWrites {

  implicit val RelationEntityPackageFormat = Json.format[RelationEntityPackage]
  implicit val RelationEntityPackageInFormat = Json.format[RelationEntityPackageIn]
  implicit val RelationEntityPackageOutFormat = Json.format[RelationEntityPackageOut]

  val relEntPack = TableQuery[RelationEntityPackageTableDef]

  implicit def toRelationEntityPackageOut(e: RelationEntityPackage): RelationEntityPackageOut = RelationEntityPackageOut(e.id, e.packageId, e.entityId, e.billed, e.created, e.optionState)

}


class RelationEntityPackageTableDef(tag: Tag) extends Table[RelationEntityPackage](tag, "package_relation_entity_package") with PKTable {

  def id          = column[UUID]("id")
  def packageId   = column[Long]("package_id")
  def entityId    = column[UUID]("entity_id")
  def billed      = column[Option[DateTime]]("billed")
  def created     = column[DateTime]("created")
  def optionState = column[Option[JsValue]]("option_state")

  override def * = (id, packageId, entityId, billed, created, optionState) <> ((RelationEntityPackage.apply _).tupled, RelationEntityPackage.unapply)

  def pk = primaryKey("package_relation_entity_package_pkey", id)


}