package com.noeupapp.middleware.entities.user

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import org.mindrot.jbcrypt.BCrypt
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json

import scala.language.postfixOps

case class Account(
                 loginInfo: api.LoginInfo,
                 user: User) extends api.Identity


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