package com.noeupapp.middleware.entities.user

import java.util.UUID

import anorm.SqlParser._
import anorm.~
import play.api.libs.json.Json
import scala.language.implicitConversions

case class User(
                 id: UUID,
                 firstName: Option[String],
                 lastName: Option[String],
                 email: Option[String],
                 avatarUrl: Option[String],
                 active: Boolean,
                 deleted: Boolean
               )

case class UserIn(
                   firstName: Option[String],
                   lastName: Option[String],
                   email: Option[String],
                   avatarUrl: Option[String]
                 )

case class UserOut(
                    id: UUID,
                    firstName: Option[String],
                    lastName: Option[String],
                    email: Option[String],
                    avatarUrl: Option[String],
                    active: Boolean
                  )


object User {

  implicit val UserFormat = Json.format[User]
  implicit val UserInFormat = Json.format[UserIn]
  implicit val UserOutFormat = Json.format[UserOut]

  val parse = {
    get[UUID]("id") ~
      get[Option[String]]("first_name") ~
      get[Option[String]]("last_name") ~
      get[Option[String]]("email") ~
      get[Option[String]]("avatar_url") ~
      get[Boolean]("active") ~
      get[Boolean]("deleted") map {
      case id ~ firstName ~ lastName ~ email ~ avatarUrl ~ active ~ deleted => {
        User(id, firstName, lastName, email, avatarUrl, active, deleted)
      }
    }
    // TODO Need to parse roles and scopes
  }

  implicit def toUserOut(u:User):UserOut = UserOut(u.id, u.firstName, u.lastName, u.email, u.avatarUrl, u.active)

}