package com.noeupapp.middleware.entities.organisation

import java.util.UUID

import anorm.SqlParser._
import anorm._
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.GlobalReadsWrites

case class Organisation(id: UUID, name: String, sub_domain: String, logo_url: String, color: String, credits: Long, deleted: Boolean)

object Organisation extends GlobalReadsWrites {

  implicit val organisationFormat = Json.format[Organisation]

  val parse = {
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
}
