package com.noeupapp.middleware.entities.relationUserRole

import java.sql.Connection
import java.util.UUID

import anorm._

class RelationUserRoleDAO {

  /**
    * Insert new role relation with user
    *
    * @param user_id
    * @param role_id
    * @param connection the implicit connection of the connection or transaction
    * @return
    */
  def addRoleToUser(role_id: UUID,user_id: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """INSERT INTO entity_relation_users_roles (role_id, user_id)
           VALUES ({role_id}::UUID, {user_id}::UUID);""")
      .on(
        'role_id -> role_id,
        'user_id -> user_id
      ).execute()
  }

  def removeRoleToUser(role_id: UUID,user_id: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """DELETE FROM entity_relation_users_roles
         WHERE role_id = {role_id}::UUID AND user_id = {user_id}::UUID;""")
      .on(
        'role_id -> role_id,
        'user_id -> user_id
      ).execute()
  }

}
