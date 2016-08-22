package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID

import anorm._
import com.mohiva.play.silhouette.api.LoginInfo
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
  def findAll(implicit connection: Connection): List[User] = {
    SQL(
      """SELECT id, first_name, last_name, email, avatar_url, active, deleted
         FROM entity_users
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
      """SELECT *
         FROM entity_users
         WHERE email = {email};""")
      .on(
      'email  -> email
      ).as(User.parse *).headOption // One email corresponds to at most one user
  }

  /**
    * Finds a user by its login info.
    *
    * @param email user email
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(email: String, clientId: String)(implicit connection: Connection): Option[User] = {
    SQL(
      """SELECT *
         FROM entity_users
         WHERE email = {email} AND owned_by_client = {client_id};""")
      .on(
        'email  -> email,
        'client_id -> clientId
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
        """SELECT id, first_name, last_name, email, avatar_url, active, deleted
           FROM entity_users
           WHERE id = {id};""")
      .on(
        'id -> userID
      ).as(User.parse *).headOption
  }

  /**
    * Insert user in entity_users
    *
    * @param user the user to insert
    * @param connection the implicit connection of the transaction
    */
  def add(user: User)(implicit connection: Connection): Boolean = {
    Logger.trace("UserDao.add...")
    val a = SQL(
      """INSERT INTO entity_users (
                      id,
                      email,
                      first_name,
                      last_name,
                      avatar_url,
                      active,
                      deleted)
        VALUES ({id},
                {email},
                {first_name},
                {last_name},
                {avatar_url},
                {active},
                {deleted});""")
      .on(
        'id -> user.id,
        'email -> user.email,
        'first_name -> user.firstName,
        'last_name -> user.lastName,
        'avatar_url -> user.avatarUrl,
        'active -> user.active,
        'deleted -> user.deleted
      ).execute()
    Logger.trace("UserDao.add OK")
    a
  }
}