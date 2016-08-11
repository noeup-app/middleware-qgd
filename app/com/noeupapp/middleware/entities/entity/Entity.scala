package com.noeupapp.middleware.entities.entity

import java.util.UUID

import play.api.libs.json.Json
import anorm.SqlParser._
import anorm._

import com.noeupapp.middleware.utils.Companion

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

  def getDBColumns(value: String): Option[String] = {
    value match {
      case "id" => Some("id")
      case "parent" => Some("parent")
      case "entityType" => Some("type")
      case "accountType" => Some("account_type")
      case _ => None
    }
  }

  val tableName = "entity_entities"
}