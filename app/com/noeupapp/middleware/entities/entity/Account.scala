package com.noeupapp.middleware.entities.entity

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.authorizationClient.login.LoginInfo
import org.mindrot.jbcrypt.BCrypt

import scala.language.postfixOps

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
                 loginInfo: Option[api.LoginInfo],
                 firstName: Option[String],
                 lastName: Option[String],
                 fullName: Option[String],
                 email: Option[String],
                 scopes: List[String],
                 roles: List[String],
                 avatarURL: Option[String],
                 deleted: Boolean) extends api.Identity



object Account {

  val parse = {
    get[UUID]("id") ~
    (LoginInfo.parse ?) ~
    get[Option[String]]("first_name") ~
    get[Option[String]]("last_name") ~
    get[Option[String]]("email") ~
    get[Option[String]]("role_name") ~
    get[Option[String]]("avatar_url") ~
    get[Boolean]("deleted") map {
      case id ~ loginInfo ~ fname ~ lname ~ email ~ role ~ avatar ~ deleted => {
        // Create full name from firstname and lastname
        val full_name: Option[String] = (fname, lname) match {
          case (Some(fn), Some(ln)) => Some(s"$fn $ln")
          case _                    => None
        }


        val roleList = role match {
          case Some(r) => List(r)
          case None    => List()
        }
        Account(id, loginInfo, fname, lname, full_name, email, List(), roleList, avatar, deleted)
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


  /**
    * @param email unique email
    * @param encryptedPassword Encrypted version of password
    * @return Option oauthIdentity.
    */
  def findByEmailAndPassword(email: String, encryptedPassword: String):Option[Account] = ???

//    DB.withConnection(implicit c =>
//    SQL(
//      """
//          SELECT *
//          FROM oauth_identity i
//          WHERE i.email = {email} AND i.password = {password}
//      """)
//      .on("email" -> email,
//        "password" -> encryptedPassword)
//      .as(parse *).headOption
//  )



  /**
    * @param id user id
    * @return Option oauthIdentity.
    */
  def findByUserId(id: UUID):Option[Account] = ???
//    DB.withConnection(implicit c =>
//    SQL(
//      """
//          SELECT *
//          FROM oauth_identity i
//          WHERE i.client_id = {client_id}
//      """)
//      .on("client_id" -> id)
//      .as(parse *).headOption
//  )


  /**
    * @param user User object
    * @param password encrypted password
    * @return
    */
  def insert(user: Account, password: String) = ???
//    DB.withConnection(implicit c =>
//    SQL("""
//          INSERT INTO oauth_identity
//            (user_id, first_name, last_name, full_name, password, email, avatar_url)
//          VALUES
//            ({user_id}, {first_name}, {last_name}, {full_name},{paswword} , {email}, {avatar_url})
//        """)
//      .on("user_id" -> user.userID,
//        "first_name"-> user.firstName,
//        "last_name" -> user.lastName,
//        "full_name" -> user.fullName,
//        "email" -> user.email,
//        "password" -> password,
//        "avatar_url" -> user.avatarURL)
//      .execute()
//  )


  /**
    * Delete the the given user from database.
    *
    * @param userId
    * @return
    */
  def delete(userId: UUID) = ???
//    DB.withConnection( implicit c =>
//    SQL("""
//      DELETE FROM oauth_identity
//      WHERE user_id = {user_id}
//        """)
//      .on(
//        "user_id" -> userId
//      ).execute()
//  )


  /**
    * Update the given client.
    *
    * @param userId
    * @return
    */
  def update(userId: UUID) = ???
//    DB.withConnection( implicit c =>
//    SQL("""
//      UPDATE auth_clients
//      SET client_id = {client_id},
//          client_name = {client_name},
//          client_secret = {client_secret},
//          description = {description},
//          redirect_uri = {redirect_uri},
//          scope = {scope}
//      WHERE client_id = {client_id}
//        """)
//      .on(
//        "client_id" -> userId)
//      .execute()
//  )

}