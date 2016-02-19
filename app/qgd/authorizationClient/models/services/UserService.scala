package qgd.authorizationClient.models.services

import java.util.UUID
import javax.inject.Inject
import play.api.Play.current
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Logger
import play.api.db.DB
import qgd.authorizationClient.models.daos.UserDAO
import qgd.resourceServer.models.Account
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Handles actions to users.
  *
  * @param userDAO The user DAO implementation.
  */
class UserService @Inject() (userDAO: UserDAO) extends IdentityService[Account] {

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = DB.withConnection({ implicit c =>
    Logger.debug("UserService.retrieve : " + loginInfo)

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: Account) = {
    Logger.debug("UserService.save : " + user)
    userDAO.save(user)
  }


    /**
      * Saves a user.
      *
      * @param user The user to save.
      * @return The saved user.
      */
    def save(user: Account): Future[Account] = DB.withTransaction({ implicit c => Future.successful {
      // Add user
      userDAO.add(user)

      // Add user login info
      userDAO.addLoginInfo(user)

      // Add relation between user and login info
      userDAO.addRelationWithUser(user)

      // Add user roles
      roleService.addUserRoles(user)

      user
    }
    })

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def save(profile: CommonSocialProfile): Future[Account] = DB.withTransaction({ implicit c => /*Future.successful*/ {
    Logger.debug("UserService.save.profile : " + profile)
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        userDAO.save(user.copy(
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new user
        userDAO.save(Account(
          id = UUID.randomUUID(),
          loginInfo = Some(profile.loginInfo),
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          email = profile.email,
          scopes = List(),
          roles = List(),
          avatarURL = profile.avatarURL,
          deleted = false
        ))
    }
  }
}
