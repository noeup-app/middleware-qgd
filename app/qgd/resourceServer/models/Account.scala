package qgd.resourceServer.models

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import qgd.authorizationClient.models.LoginInfo

/**
 * The user object.
 *
 * @param id The unique ID of the user.
 * @param loginInfo The linked login info.
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName Maybe the last name of the authenticated user.
 * @param fullName Maybe the full name of the authenticated user.
 * @param email Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 */
case class Account(
                 id: UUID,
                 loginInfo: api.LoginInfo,
                 firstName: Option[String],
                 lastName: Option[String],
                 fullName: Option[String],
                 email: Option[String],
                 scopes: List[String],
                 roles: List[String],
                 avatarURL: Option[String]) extends api.Identity


object Account {

  val parse = {
    get[UUID]("id") ~
    LoginInfo.parse ~
    get[Option[String]]("first_name") ~
    get[Option[String]]("last_name") ~
    get[Option[String]]("email") ~
    get[Option[String]]("role_name") ~
    get[Option[String]]("avatar_url") map {
      case id ~ loginInfo ~ fname ~ lname ~ email ~ role ~ avatar => {
        val full_name: Option[String] = (fname, lname) match {
          case (Some(fn), Some(ln)) => Some(s"$fn $ln")
          case _                    => None
        }
        val roleList = role match {
          case Some(r) => List(r)
          case None    => List()
        }
        Account(id, loginInfo, fname, lname, full_name, email, List(), roleList, avatar)
      }
    }

    // TODO Need to parse roles and scopes

  }

}