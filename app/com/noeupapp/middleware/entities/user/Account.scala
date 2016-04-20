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


  /**
    * Encrypt the clear password using b-crypt
    *
    * @param password  the clear password to encrypt
    * @return          the hashed password and the salt used
    */
  def encryptPassword(password: String): (String, Option[String]) = {
    val salt = BCrypt.gensalt(10)
    val hash = BCrypt.hashpw(password, salt)
    (hash, Some(salt))
  }
}