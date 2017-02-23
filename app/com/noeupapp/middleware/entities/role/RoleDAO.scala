package com.noeupapp.middleware.entities.role

import java.sql.Connection
import java.util.UUID

import anorm._
import anorm.SqlParser._
import com.noeupapp.middleware.entities.user.User

import scala.language.postfixOps

class RoleDAO {

  /**
    * Insert New role in DB
    * Set role_name as unique makes it easier to manage without using Enum for the moment
    *
    * @param user_id the user to to link with the role
    * @param role String
    * @param connection the implicit connection of the connection or transaction
    */
  // TODO make Role an ENUM or not?
  def addRole(user_id: UUID, role: String)(implicit connection: Connection): Boolean = {
    val roleId = UUID.randomUUID()
    SQL(
        """INSERT INTO entity_roles (id, role_name)
             VALUES ({id}, {role_name});""")
    .on(
          'id -> roleId,
          'role_name -> role
    ).execute()
  }



  def getByUserId(userId: UUID)(implicit connection: Connection): List[String] = {
    SQL(
      """
        SELECT role_name
        FROM entity_roles role
        INNER JOIN entity_relation_users_roles user_role ON user_role.role_id = role.id
        WHERE user_role.user_id = {userId}::UUID;
      """.stripMargin)
      .on(
        'userId -> userId
      ).as(scalar[String] *)
  }


  def getIdByRoleName(name: String)(implicit connection: Connection): Option[UUID] = {
    SQL(
      """
        SELECT id
        FROM entity_roles role
        WHERE role_name = {name};
      """)
      .on(
        'name -> name
      ).as(scalar[UUID].singleOpt)
  }

}
