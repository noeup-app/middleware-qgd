package qgd.authorizationClient.models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import qgd.resourceServer.models.Account

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
trait UserDAO {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[Account]]

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID): Future[Option[Account]]

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: Account): Future[Account]
}
