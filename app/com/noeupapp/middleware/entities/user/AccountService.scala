package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.entities.role.RoleService
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Handles actions to users.
  */
class AccountService @Inject()(userService: UserService, roleService: RoleService) extends IdentityService[Account] {

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = {
    println("------------------------AccountService.retrieve : " + loginInfo)
    DB.withConnection({ implicit c =>
      // Cas particulier de l'utilisation de l'Expect. gérer l'erreur avec eventbus et retourner une Future
      userService.findByEmail(loginInfo.providerKey).map{
        case Some(user) =>
          Logger.debug("AccountService.retrieve : no user found with " + loginInfo)
          Some(Account(loginInfo, user))
        case None =>
          Logger.debug("userDAO.find : no user found with " + loginInfo)
          None
      }
    })
  }


  /**
    * Saves a user.
    *
    * @param account The user to save.
    * @return The saved user.
    */
  def save(account: Account): Future[Account] = {
    DB.withTransaction({ implicit c =>

      for{
        userSuccessfullyAdded <- userService.add(account.user)
//        _ <- userDAO.addLoginInfo(account)
        roleSuccessfullyAdded <- roleService.addUserRoles(account)
      } yield {
        if(userSuccessfullyAdded && roleSuccessfullyAdded) {
          Logger.debug("Account saved")
          account
        }else {
          val errorMessageUser = if(userSuccessfullyAdded) "Error while saving user" else ""
          val errorMessageRole = if(roleSuccessfullyAdded) "Error while saving role" else ""

          throw new Exception(s"An error occurred when saving account : $errorMessageUser | $errorMessageRole")
        }
      }
    })
  }

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def save(profile: CommonSocialProfile): Future[Account] = { DB.withTransaction({ implicit c =>
    Logger.debug("AccountService.save(" + profile + ")")

    userService.findByEmail(profile.loginInfo.providerKey) flatMap  {
      case Some(user) => // Update user with profile
        val account = Account(
          profile.loginInfo,
          user.copy(
            firstName = profile.firstName,
            lastName = profile.lastName,
            email = profile.email,
            avatarUrl = profile.avatarURL
          )
        )
        save(account)
      case None => // Insert a new user
        val account = Account(
          loginInfo = profile.loginInfo,
          user = User(
            id = UUID.randomUUID(),
            firstName = profile.firstName,
            lastName = profile.lastName,
            email = profile.email,
            avatarUrl = profile.avatarURL,
            active = false,
            deleted = false
          )
        )
        save(account)
    }
  })}
}
