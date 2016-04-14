package com.noeupapp.middleware.entities.role

import java.sql.Connection
import java.util.UUID

import anorm._

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

}
