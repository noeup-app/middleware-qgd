package qgd.authorizationClient.models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Logger
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
  def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = {
    // TODO cas particulier de l'utilisation de l'Expect ? gérer l'erreur avec eventbus
    Logger.debug("UserService.retrieve : " + loginInfo)
    userDAO.find(loginInfo)
  }

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
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def save(profile: CommonSocialProfile) = {
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
