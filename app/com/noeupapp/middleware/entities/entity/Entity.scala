package com.noeupapp.middleware.entities.entity

import java.util.UUID

import play.api.libs.json.Json
import anorm.SqlParser._
import anorm._

case class Entity(id: UUID, parent: Option[UUID], entityType: String, accountType: Option[String])

case class EntityOut(id: UUID,
                     firstName: Option[String],
                     lastName: Option[String],
                     organisationName: Option[String])


object Entity {
  implicit val EntityFormat = Json.format[Entity]
  implicit val EntityOutFormat = Json.format[EntityOut]

  val parse = {
    get[UUID]("id") ~
    get[Option[UUID]]("parent") ~
    get[String]("type") ~
    get[Option[String]]("account_type") map {
      case id ~ parent ~ entityType ~ accountType => Entity(id, parent, entityType, accountType)
    }
  }

  val parseOut = {
    get[UUID]("id") ~
      get[Option[String]]("first_name") ~
      get[Option[String]]("last_name") ~
      get[Option[String]]("organisation_name") map {
      case id ~ firstName ~ lastName ~ organisationName =>
        EntityOut(id, firstName, lastName, organisationName)
    }
  }
}