package qgd.authorizationClient.models.daos

import java.sql.Connection
import java.util.UUID

import anorm._
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.Logger
import play.api.db.DB
import qgd.authorizationClient.utils.GlobalReadsWrites
import qgd.resourceServer.models.Account
import play.api.Play.current
import scala.concurrent.Future
import scala.language.postfixOps


/**
  * Give access to the user object.
  */
class UserDAO extends GlobalReadsWrites {

  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(loginInfo: LoginInfo)(implicit connection: Connection): List[Account] = {
    SQL(
      """SELECT u.id, r_li_u.provider_id, r_li_u.provider_key, u.first_name, u.last_name, u.email, r.role_name, u.avatar_url, u.deleted
          FROM entity_users u
          INNER JOIN entity_relation_login_infos_users r_li_u ON u.id = r_li_u.user_id
          LEFT JOIN entity_relation_users_roles r_u_r ON u.id = r_u_r.user_id
          LEFT JOIN entity_roles r ON r.id = r_u_r.role_id
          WHERE r_li_u.provider_id = {provider_id} AND r_li_u.provider_key = {provider_key};
                                 """)
      .on(
      'provider_id ->
        loginInfo.providerID,
      'provider_key -> loginInfo.providerKey
      )
      .as(


        Account.parse *)
  }


  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID)(implicit connection:
  Connection): List[Account] = {
      SQL(
        """SELECT u.id, r_li_u.provider_id, r_li_u.provider_key, u.first_name, u.last_name, u.email, r.role_name, u.avatar_url, u.deleted
          FROM entity_users u
          INNER JOIN entity_relation_login_infos_users r_li_u ON u.id = r_li_u.user_id
          LEFT JOIN entity_relation_users_roles r_u_r ON u.id = r_u_r.user_id
          INNER JOIN entity_roles r ON r.id = r_u_r.role_id
          WHERE u.id = {user_id};
                                 """)
      .on(
        'user_id -> userID)
        .as(Account.parse *)
  }

  /**
    * Insert user in entity_users
    *
    * @param user the user to insert
    * @param connection the implicit connection of the transaction
    */
  def add(user: Account)(implicit connection: Connection) = {
    SQL(
      """INSERT INTO entity_users (id, first_name, last_name, email, avatar_url, base_node)
         (SELECT {id}, {first_name}, {last_name}, {email}, {avatar_url}, {base_node}
         WHERE NOT EXISTS (SELECT id FROM entity_users WHERE id = {id}));""")
      .on(
        'id -> user.id,
        'first_name -> user.firstName,
        'last_name -> user.lastName,
        'email -> user.email,
        'avatar_url -> user.avatarURL,
        'base_node -> new UUID(0, 0) // TODO change new UUID(0,0)
      ).execute()
  }

  /**
    * Insert :
    *   - role name
    *   - relation between user and role
    *
    * @param user the user to to link with the role
    * @param connection the implicit connection of the transaction
    */
  def addUserRoles(user: Account)(implicit connection: Connection) = {
    def addUserRole(user_id: UUID, role: String): Boolean = {
      val roleId = UUID.randomUUID()
      SQL(
        """INSERT INTO entity_roles (id, role_name)
           VALUES ({id}, {role_name});""")
        .on(
          'id -> roleId,
          'role_name -> role
        ).execute()

      SQL(
        """INSERT INTO entity_relation_users_roles (role_id, user_id)
           VALUES ({role_id}, {user_id});""")
        .on(
          'role_id -> roleId,
          'user_id -> user_id
        ).execute()
    }
    user.roles.map(addUserRole(user.id, _))
  }

  /**
    * Insert :
    *   - login info
    *   - relation between user and login info
    *
    * @param user the user that contains login infos
    * @param connection the implicit connection of the transaction
    */
  def addLoginInfo(user: Account)(implicit connection: Connection) = {
    // Add login info
    user.loginInfo match {
      case Some(loginInfo) =>
        Logger.warn(loginInfo.toString)
        SQL(
          """INSERT INTO entity_login_infos (provider_id, provider_key)
         (SELECT {provider_id}, {provider_key}
         WHERE NOT EXISTS (SELECT * FROM entity_login_infos WHERE provider_id = {provider_id} AND provider_key = {provider_key}));""")
          .on(
            'provider_id -> loginInfo.providerID,
            'provider_key -> loginInfo.providerKey
          ).execute()

        SQL(
          """INSERT INTO entity_relation_login_infos_users (provider_id, provider_key, user_id)
         (SELECT {provider_id}, {provider_key}, {user_id}
         WHERE NOT EXISTS (SELECT * FROM entity_relation_login_infos_users WHERE provider_id = {provider_id} AND provider_key = {provider_key} AND user_id = {user_id}));""")
          .on(
            'provider_id -> loginInfo.providerID,
            'provider_key -> loginInfo.providerKey,
            'user_id -> user.id
          ).execute()
      case None =>
    }
  }
}