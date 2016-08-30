package com.noeupapp.middleware.entities.group

import java.util.UUID

import play.api.libs.json.Json
import anorm.SqlParser._
import anorm._
import com.noeupapp.middleware.entities.entity.EntityOut
import com.noeupapp.middleware.entities.entity.Entity._

import scala.language.{implicitConversions, postfixOps}


/**
  * A group of entities
  *
  * @param id
  * @param name
  * @param owner
  * @param deleted
  */
case class Group(
                 id: UUID,
                 name: String,
                 owner: UUID,
                 deleted: Boolean = false
                )

/**
  * Input to create a group
  *
  * @param name
  */
case class GroupIn(
                  name: String,
                  owner: Option[UUID]
                  )
/**
  * Output for  group members
  *
  * @param groupName
  */
case class GroupMembers(
                 groupName: String,
                 members: List[EntityOut]
                 )

object Group{
  implicit val GroupFormat = Json.format[Group]
  implicit val GroupInFormat = Json.format[GroupIn]
  implicit val GroupMemberFormat = Json.format[GroupMembers]

  val parse = {
    get[UUID]("id") ~
      get[String]("name") ~
      get[UUID]("owner") ~
      get[Boolean]("deleted") map {
      case id ~ name ~ owner ~ deleted => Group(id, name, owner, deleted)
    }
  }
}