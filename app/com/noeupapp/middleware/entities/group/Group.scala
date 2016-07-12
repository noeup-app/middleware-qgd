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
case class GroupMember(
                 entityId: UUID,
                 firstName: Option[String],
                 lastName: Option[String],
                 organisationName: Option[String],
                 groupName: Option[String]
                 )
object Group{
  implicit val GroupFormat = Json.format[Group]
  implicit val GroupInFormat = Json.format[GroupIn]
  implicit val groupMemberFormat = Json.format[GroupMember]

  val parse = {
    get[UUID]("id") ~
      get[String]("name") ~
      get[UUID]("owner") ~
      get[Boolean]("deleted") map {
      case id ~ name ~ owner ~ deleted => Group(id, name, owner, deleted)
    }
  }

  val parseMember = {
    get[UUID]("id") ~
    get[Option[String]]("first_name") ~
    get[Option[String]]("last_name") ~
    get[Option[String]]("organisation_name") ~
    get[Option[String]]("group_name") map {
      case id ~ firstName ~ lastName ~ organisationName ~ groupName =>
        GroupMember(id, firstName, lastName, organisationName, groupName)
    }

  }
}