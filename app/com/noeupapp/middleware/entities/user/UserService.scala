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
import qgd.middleware.models.Account

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Handles actions to users.
  *
  * @param userDAO The user DAO implementation.
  */
class UserService @Inject() (userDAO: UserDAO, roleService: RoleService) extends IdentityService[Account] {

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = Future {
    DB.withConnection({ implicit c =>
      Logger.debug("UserService.retrieve : " + loginInfo)
      // Cas particulier de l'utilisation de l'Expect. gérer l'erreur avec eventbus et retourner une Future
      // TODO factoriser
      groupRolesByAccount(userDAO.find(loginInfo)) match {
        // TODO send log to event bus
        case None =>
          Logger.debug("userDAO.find : no user found with " + loginInfo)
          None
        case Some(account) => Some(account)
      }
      // TODO Case Future fails -> dans la factorisation ?
    })
  }

  /**
    * group DAO find account result by role
    *
    * @param list all records for an account (with several roles)
    * @return
    */
  def groupRolesByAccount(list: List[Account]): Option[Account] = {
    val res = {
      list
        .groupBy(_.id)
        .values.filter(_.nonEmpty).map { u =>
        val roles = u.flatMap(_.roles)
        u.head.copy(roles = roles)
      }
    }
    res.headOption
  }


  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: Account): Future[Account] = Future { DB.withTransaction({ implicit c =>
    save(user)
  })}

  private def save(user: Account)(implicit c: Connection): Account = {
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

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  def save(profile: CommonSocialProfile): Future[Account] = Future { DB.withTransaction({ implicit c =>
    Logger.debug("UserService.save.profile : " + profile)
    groupRolesByAccount(userDAO.find(profile.loginInfo)) match {
      case Some(user) => // Update user with profile
        save(user.copy(
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new user
        save(Account(
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
  })}
}
