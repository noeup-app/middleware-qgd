package com.noeupapp.middleware.entities.organisation

import java.util.UUID

import anorm.SqlParser._
import anorm._
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.GlobalReadsWrites

case class Organisation(id: UUID, name: String, sub_domain: String, logo_url: String, color: String, credits: Long, deleted: Boolean)

case class OrganisationIn(name: String, sub_domain: String, logo_url: String, color: String, credits: Long)

object Organisation extends GlobalReadsWrites {

  implicit val organisationFormat = Json.format[Organisation]
  implicit val OrganisationInFormat = Json.format[OrganisationIn]

  val parse = { // TODO check usage
    get[UUID]("id") ~
    get[String]("name") ~
    get[String]("sub_domain") ~
    get[String]("logo_url") ~
    get[String]("color") ~
    get[Long]("credits") ~
    get[Boolean]("deleted") map {
      case id ~ name ~ sub_domain ~ logo_url ~ color ~ credits ~ deleted => Organisation(id, name, sub_domain, logo_url, color, credits, deleted)
    }
  }

  val parseDB = {
    get[UUID]("id") ~
    get[String]("name") ~
    get[String]("subdomain") ~
    get[Option[String]]("logo_url") ~
    get[Option[String]]("color") ~
    get[Long]("credits") ~
    get[Option[Boolean]]("deleted") map { // TODO fix that (options)
      case id ~ name ~ sub_domain ~ logo_url ~ color ~ credits ~ deleted => Organisation(id, name, sub_domain, logo_url.getOrElse(""), color.getOrElse("black"), credits, deleted.getOrElse(false))
    }
  }
}
