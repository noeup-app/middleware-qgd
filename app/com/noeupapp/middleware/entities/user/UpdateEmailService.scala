package com.noeupapp.middleware.entities.user

import java.util.UUID

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import com.noeupapp.middleware.authorizationClient.authInfo.PasswordInfoDAO
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmailService
import com.noeupapp.middleware.authorizationClient.loginInfo.AuthLoginInfoService
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.errorHandle.ExceptionEither.tryFutures
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger
import play.api.mvc.Results.{BadRequest, Forbidden}
import com.noeupapp.middleware.utils.TypeCustom._

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}

import com.noeupapp.middleware.utils.FutureFunctor._

/**
  * Created by damien on 06/06/2017.
  */
class UpdateEmailService @Inject()(userService: UserService,
                                   authLoginInfoService: AuthLoginInfoService,
                                   passwordInfoDAO: PasswordInfoDAO,
                                   confirmEmailService: ConfirmEmailService){

  def updateEmail(id: UUID, account: Account, newEmail: String): Future[Expect[Option[Unit]]] = {

    if (isUpdatingSameEmail(account, newEmail)) {
      Logger.debug(s"Trying to update user with the same email (${account.user.email} == $newEmail). Useless, aborting...")
      return Future.successful(\/-(None))
    }

    if (isUserAllowedToUpdateEmail(id, account)){
      return Future.successful(-\/(FailError("Not allowed", errorType = Forbidden)))
    }

    for {
      // Check if email is already used
      userFromEmail <- EitherT(userService.findByEmail(newEmail))
      _             <- EitherT(userFromEmail.isEmpty |> ("Email is already used", BadRequest))

      // USERS
      _       <- EitherT(updateUser(id, newEmail))

      userEmail: String = account.user.email.getOrElse("")
      loginInfo = LoginInfo("credentials", userEmail)

      // AuthLoginInfo
      _       <- EitherT(updateLoginInfo(loginInfo, userEmail, newEmail))

      // PasswordInfo
      _ <- EitherT(updatePasswordInfo(loginInfo, newEmail))

      _ <- EitherT(userService.changeActiveStatus(id, status = false))

      _ <- EitherT(confirmEmailService.sendEmailConfirmation(newEmail))
    } yield Some(())
  }.run


  private def isUpdatingSameEmail(account: Account, newEmail: String): Boolean =
    account.user.email.contains(newEmail)


  private def isUserAllowedToUpdateEmail(id: UUID, account: Account) =
    id != account.user.id && !account.roles.contains("admin")


  private def updateUser(id: UUID, newEmail: String): Future[Expect[Unit]] = {
    for{
      userOpt <- EitherT(userService.findById(id))
      user    <- EitherT(userOpt |> "User not found")
      _       <- EitherT(userService.update(id, user.copy(email = Some(newEmail))))
    } yield ()
  }.run


  private def updateLoginInfo(loginInfo: LoginInfo, userEmail: String, newEmail: String): Future[Expect[Unit]] = {
    for {
      authLoginInfoOpt <- EitherT(authLoginInfoService.find(loginInfo))
      authLoginInfo    <- EitherT(authLoginInfoOpt |> s"AuthLoginInfo (credentials, $userEmail) is not found")
      _                <- EitherT(authLoginInfoService.update(loginInfo, authLoginInfo.copy(providerKey = newEmail)))
    } yield ()
  }.run


  private def updatePasswordInfo(loginInfo: LoginInfo, newEmail: String): Future[Expect[Unit]] = {
    for {
      passwordInfoOpt <- EitherT(tryFutures(passwordInfoDAO.find(loginInfo)))
      passwordInfo    <- EitherT(passwordInfoOpt |> "Password info is not found")
      _ <- EitherT(tryFutures(passwordInfoDAO.add(loginInfo.copy(providerKey = newEmail), passwordInfo)))
      _ <- EitherT(tryFutures(passwordInfoDAO.remove(loginInfo)))
    } yield ()
  }.run
}
