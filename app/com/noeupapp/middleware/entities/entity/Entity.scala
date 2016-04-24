package com.noeupapp.middleware.entities.entity

import java.util.UUID

import play.api.libs.json.Json
import anorm.SqlParser._
import anorm._

case class Entity(id: UUID, parent: Option[UUID], entityType: String, accountType: Option[String])


object Entity {
  implicit val EntityFormat = Json.format[Entity]

  val parse = {
    get[UUID]("id") ~
    get[Option[UUID]]("parent") ~
    get[String]("type") ~
    get[Option[String]]("account_type") map {
      case id ~ parent ~ entityType ~ accountType => Entity(id, parent, entityType, accountType)
    }
  }
}