package com.noeupapp.middleware.packages.pack

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import play.api.libs.json.{JsValue, Json}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import com.noeupapp.middleware.utils.GlobalReadsWrites

import scala.language.{implicitConversions, postfixOps}



case class Pack(id: Long,
                  name: String,
                  optionOffer: Option[JsValue]) extends Entity[Long] {

  override def withNewId(id: Long): Entity[Long] = copy(id = id)

  def this(e: PackIn) = this(0, e.name, e.optionOffer)

}


case class PackIn(name: String,
                     optionOffer: Option[JsValue])

case class PackOut(id: Long,
                      name: String,
                      optionOffer: Option[JsValue])

object Pack extends GlobalReadsWrites {

  implicit val PackFormat    = Json.format[Pack]
  implicit val PackInFormat  = Json.format[PackIn]
  implicit val PackOutFormat = Json.format[PackOut]

  val pack = TableQuery[PackTableDef]

  implicit def toPackOut(e: Pack): PackOut = PackOut(e.id, e.name, e.optionOffer)

}


class PackTableDef(tag: Tag) extends Table[Pack](tag, "package_packages") with PKTable {

  def id          = column[Long]("id", O.AutoInc)
  def name        = column[String]("name")
  def optionOffer = column[Option[JsValue]]("option_offer")
  override def *  = (id, name, optionOffer) <> ((Pack.apply _).tupled, Pack.unapply)

  def pk = primaryKey("package_packages_pkey", id)


}

