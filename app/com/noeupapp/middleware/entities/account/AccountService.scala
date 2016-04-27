package com.noeupapp.middleware.entities.account

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.entities.organisation.{Organisation, OrganisationService}
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.entities.user.{User, UserService}
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, EitherT, OptionT, \/-}
import com.noeupapp.middleware.utils.FutureFunctor._


/**
  * Handles actions to users.
  */
class AccountService @Inject()(userService: UserService,
                               roleService: RoleService,
                               organisationService: OrganisationService)
  extends IdentityService[Account] {

  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[Account]] = {
      {
        for {
          user         <- EitherT(userService.findByEmailEither(loginInfo.providerKey))
          organisation <- EitherT(userService.findOrganisationByUserId(user.id))
        } yield Account(loginInfo, user, organisation)
      }.run map {
        case -\/(e) =>
          Logger.error(s"User not found $e")
          None
        case \/-(res) => Some(res)
      }
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
        //userSuccessfullyAdded <- userService.add(account.user)
//        _ <- userDAO.addLoginInfo(account)
        roleSuccessfullyAdded <- roleService.addUserRoles(account)
      } yield {
        if(/*userSuccessfullyAdded && */roleSuccessfullyAdded) {
          Logger.debug("Account saved")
          account
        }else {
          //val errorMessageUser = if(userSuccessfullyAdded) "Error while saving user" else ""
          val errorMessageRole = if(roleSuccessfullyAdded) "Error while saving role" else ""

          Logger.error("An error occurred when saving account : "/*$errorMessageUser |*/ + errorMessageRole)
          account
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
          ),
          None
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
          ),
          None
        )
        save(account)
    }
  })}
}
