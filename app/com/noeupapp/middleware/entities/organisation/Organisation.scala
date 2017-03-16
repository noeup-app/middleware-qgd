package com.noeupapp.middleware.entities.organisation

import java.util.UUID

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.GlobalReadsWrites
import com.noeupapp.middleware.crudauto.{Entity, PKTable}

import scala.language.{implicitConversions, postfixOps}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._


case class Organisation(id: UUID, 
                        name: String, 
                        sub_domain: String, 
                        logo_url: Option[String],
                        color: Option[String],
                        credits: Long, 
                        created: DateTime, 
                        deleted: Boolean) extends Entity[UUID] {

  def this(e: OrganisationIn) = this(UUID.randomUUID(), e.name, e.sub_domain, e.logo_url, e.color, e.credits, DateTime.now(), deleted = false)

  override def withNewId(id: UUID): Entity[UUID] = copy(id = id)

}


case class OrganisationIn(name: String, 
                          sub_domain: String,
                          logo_url: Option[String],
                          color: Option[String],
                          credits: Long)


case class OrganisationOut(id: UUID,
                           name: String,
                           sub_domain: String,
                           logo_url: Option[String],
                           color: Option[String],
                           credits: Long,
                           created: DateTime)


object Organisation extends GlobalReadsWrites {

  implicit val OrganisationFormat    = Json.format[Organisation]
  implicit val OrganisationInFormat  = Json.format[OrganisationIn]
  implicit val OrganisationOutFormat = Json.format[OrganisationOut]

  val organisation = TableQuery[OrganisationTableDef]

  val parseDB = {
    get[UUID]("id") ~
      get[String]("name") ~
      get[String]("subdomain") ~
      get[Option[String]]("logo_url") ~
      get[Option[String]]("color") ~
      get[Long]("credits") ~
      get[DateTime]("created") ~
      get[Boolean]("deleted") map { // TODO fix that (options)
      case id ~ name ~ sub_domain ~ logo_url ~ color ~ credits ~ created ~ deleted => Organisation(id, name, sub_domain, logo_url, color, credits, created, deleted)
    }
  }

  implicit def toOrganisationOut(e: Organisation): OrganisationOut =
    OrganisationOut(e.id, e.name, e.sub_domain, e.logo_url, e.color, e.credits, e.created)
}


class OrganisationTableDef(tag: Tag) extends Table[Organisation](tag, "entity_organisations") with PKTable {


  def id        = column[UUID]("id")
  def name      = column[String]("name")
  def subDomain = column[String]("subdomain")
  def logoUrl   = column[Option[String]]("logo_url")
  def color     = column[Option[String]]("color")
  def credits   = column[Long]("credits")
  def created   = column[DateTime]("created")
  def deleted   = column[Boolean]("deleted")

  override def * = (id, name, subDomain, color, logoUrl, credits, created, deleted) <> ((Organisation.apply _).tupled, Organisation.unapply)

  def pk = primaryKey("organisation_pk", id)


}