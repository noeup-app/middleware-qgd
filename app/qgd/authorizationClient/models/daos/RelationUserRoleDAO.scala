package qgd.authorizationClient.models.daos

import java.sql.Connection
import java.util.UUID

import anorm._

class RelationUserRoleDAO {

  /**
    * Insert new role relation with user
    *
    * @param user_id
    * @param roleName
    * @param connection the implicit connection of the connection or transaction
    * @return
    */
  def addRoleToUser(user_id: UUID, roleName: String)(implicit connection: Connection): Boolean = {
    SQL(
      """INSERT INTO entity_relation_users_roles (role_name, user_id)
           VALUES ({role_name}, {user_id});""")
      .on(
        'role_name -> roleName,
        'user_id -> user_id
      ).execute()
  }

}
