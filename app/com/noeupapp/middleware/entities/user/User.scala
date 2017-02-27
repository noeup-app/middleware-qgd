package com.noeupapp.middleware.entities.user

import java.util.UUID

import anorm.SqlParser._
import anorm.~
import org.joda.time.DateTime
import play.api.libs.json.Json
import scala.language.implicitConversions

case class User(
                 id: UUID,
                 firstName: Option[String],
                 lastName: Option[String],
                 email: Option[String],
                 avatarUrl: Option[String],
                 created: DateTime,
                 active: Boolean,
                 deleted: Boolean,
                 ownedByClient: Option[String]
               ) {

  lazy val toUserIn: UserIn =
    UserIn(
      firstName,
      lastName,
      email,
      avatarUrl,
      ownedByClient
    )
}

case class UserIn(
                   firstName: Option[String],
                   lastName: Option[String],
                   email: Option[String],
                   avatarUrl: Option[String],
                   ownedByClient: Option[String] = None
                  ) {

  lazy val toUser: User =
    User(
      UUID.randomUUID(),
      firstName,
      lastName,
      email,
      avatarUrl,
      DateTime.now(),
      active = true,
      deleted = false,
      ownedByClient
    )

  lazy val toNotActivatedUser: User = toUser.copy(active = false)

}

case class UserOut(
                    id: UUID,
                    firstName: Option[String],
                    lastName: Option[String],
                    email: Option[String],
                    avatarUrl: Option[String],
                    created: DateTime,
                    active: Boolean,
                    ownedByClient: Option[String]
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
      get[DateTime]("created") ~
      get[Boolean]("active") ~
      get[Boolean]("deleted") ~
      get[Option[String]]("owned_by_client") map {
      case id ~ firstName ~ lastName ~ email ~ avatarUrl ~ created ~ active ~ deleted ~ ownedByClient=> {
        User(id, firstName, lastName, email, avatarUrl, created, active, deleted, ownedByClient)
      }
    }
    // TODO Need to parse roles and scopes
  }

  implicit def toUserOut(u:User):UserOut = UserOut(u.id, u.firstName, u.lastName, u.email, u.avatarUrl, u.created, u.active, u.ownedByClient)

  implicit def toUser(u:UserOut):User = User(u.id, u.firstName, u.lastName, u.email, u.avatarUrl, u.created, u.active, deleted = false, u.ownedByClient)





  // Bypass because of nulab/scala-oauth2-provider lib (
  // when using client credential flow, nulab lib need to link client with user which is not the RFC requirement
  // An issue is pending on github and Damien is requesting a MR
  private val defaultUser: User = User(new UUID(0, 0), Some("FAKE"), Some("FAKE"), Some("FAKE"), Some("FAKE"), DateTime.now(),active = true, deleted = false, ownedByClient = Some("FAKE"))

  def getDefault = defaultUser
  def isDefault(u: User) = defaultUser.equals(u)


}