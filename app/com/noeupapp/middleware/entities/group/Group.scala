package com.noeupapp.middleware.entities.group

import java.util.UUID

import play.api.libs.json.Json
import anorm.SqlParser._
import anorm._

import scala.language.{implicitConversions, postfixOps}

case class Group(
                 id: UUID,
                 name: String,
                 owner: UUID,
                 deleted: Boolean = false
                )

case class GroupIn(
                  name: String
                  )
object Group{
  implicit val GroupFormat = Json.format[Group]
  implicit val GroupInFormat = Json.format[GroupIn]

  val parse = {
    get[UUID]("id") ~
      get[String]("name") ~
      get[UUID]("owner") ~
      get[Boolean]("deleted") map {
      case id ~ name ~ owner ~ deleted => Group(id, name, owner, deleted)
    }
  }
}