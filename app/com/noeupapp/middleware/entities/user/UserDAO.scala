package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID

import anorm._
import anorm.SqlParser._
import play.api.Logger
import com.noeupapp.middleware.utils.GlobalReadsWrites

import scala.language.postfixOps


/**
  * Give access to the user object.
  */
class UserDAO extends GlobalReadsWrites {

  /**
    * Finds all users in DB.
    *
    * @return
    */
  def findAll(email:Option[String])(implicit connection: Connection): List[User] = {
    val condition = email match {
      case Some(emailVal) => "WHERE email = '"+emailVal+"'"
      case None => ""
    }
    SQL(
      s"""SELECT *
         FROM entity_users
         $condition
      """)
      .as(User.parse *)

  }

  /**
    * Finds a user by its login info.
    *
    * @param email user email
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(email: String)(implicit connection: Connection): Option[User] = {
    SQL(
      """SELECT id, first_name, last_name, email, avatar_url, created, active, deleted, owned_by_client
         FROM entity_users
         WHERE email = {email} AND deleted = 'false';""")
      .on(
      'email  -> email
      ).as(User.parse *).headOption // One email corresponds to at most one user
  }

  /**
    * Finds a user by its login info.
    *
    * 'active' field have to be true in order to connect the user
    *
    * @param email user email
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(email: String, clientId: String)(implicit connection: Connection): Option[User] = {
    SQL(
      """SELECT *
         FROM entity_users
         WHERE email = {email} AND owned_by_client = {client_id} AND active = 'true';""")
      .on(
        'email  -> email,
        'client_id -> clientId
      ).as(User.parse *).headOption // One email corresponds to at most one user
  }

  /**
    * Find inactive user for sending an email confirmation
    *
    * @param email
    * @param connection
    * @return
    */
  def findInactive(email: String)(implicit connection: Connection): Option[User] = {
    SQL(
      """SELECT *
         FROM entity_users
         WHERE email = {email} AND active = 'false';""")
      .on(
        'email  -> email
      ).as(User.parse *).headOption // One email corresponds to at most one user
  }



  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID)(implicit connection:
  Connection): Option[User] = {
      SQL(
        """SELECT id, first_name, last_name, email, avatar_url, created, active, deleted,owned_by_client
           FROM entity_users
           WHERE id = {id};""")
      .on(
        'id -> userID
      ).as(User.parse *).headOption
  }

  /**
    *
    * @param userId User UUID
    * @param status active status (true/false)
    * @param connection the implicit connection of the transaction
    * @return
    */
  def updateActive(userId: UUID, status: Boolean)(implicit connection: Connection): Boolean = {
    SQL(
      """
          UPDATE entity_users
          SET active = {status}
          WHERE id = {userId}
      """
    ).on(
      'userId -> userId,
      'status -> status
    ).execute()
  }

  /**
    *
    * Count active users un db
    * @return
    */
  def countActiveUsers()(implicit connection: Connection): Int = {
    SQL(
      """
          SELECT COUNT(*) FROM entity_users
          WHERE active = true
      """
    ).as(scalar[Int].single)
  }

  /**
    * Insert user in entity_users
    *
    * @param user the user to insert
    * @param connection the implicit connection of the transaction
    */
  def add(user: User)(implicit connection: Connection): Boolean = {
    Logger.trace(s"UserDao.add... $user")
    val a = SQL(
      """INSERT INTO entity_users (
                      id,
                      email,
                      first_name,
                      last_name,
                      avatar_url,
                      active,
                      deleted,
                      owned_by_client)
        VALUES ({id},
                {email},
                {first_name},
                {last_name},
                {avatar_url},
                {active},
                {deleted},
                {owned_by_client});""")
      .on(
        'id -> user.id,
        'email -> user.email,
        'first_name -> user.firstName,
        'last_name -> user.lastName,
        'avatar_url -> user.avatarUrl,
        'active -> user.active,
        'deleted -> user.deleted,
        'owned_by_client -> user.ownedByClient
      ).execute()
    Logger.trace("UserDao.add OK")
    a
  }

  /**
    * Change user delete value to true
    * @param userId user to archive
    * @param connection the implicit connection of the transaction
    * @return
    */
  def delete(userId: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
          UPDATE entity_users
          SET deleted = 'true'
          WHERE id = {id}
        """
    ).on(
      'id -> userId
    ).execute()
  }
}