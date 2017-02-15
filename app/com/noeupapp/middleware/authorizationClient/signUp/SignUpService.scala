package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api._
import com.noeupapp.middleware.authorizationClient.signUp.SignUpForm.Data
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.entities.user.{UserIn, UserService}
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.{-\/, \/-}

/**
  *
  * @param accountService     The account service implementation.
  * @param passwordHasher     The password hasher implementation.
  * @param userService        The user service implementation.
  * @param authInfoRepository The auth info repository implementation.
  */
class SignUpService @Inject()(accountService: AccountService,
                              passwordHasher: PasswordHasher,
                              userService: UserService,
                              authInfoRepository: AuthInfoRepository
                             ) {

  def signUp(loginInfo: LoginInfo, data: Data, authorizationResult: SignUpsResult): Future[Expect[(Account)]] = {
    accountService.retrieve(loginInfo).flatMap {
      case Some(user) => Future.successful(-\/(FailError("User already exist")))

      case None =>
        val authInfo = passwordHasher.hash(data.password)
        val newUser = UserIn(
          firstName = Some(data.firstName),
          lastName  = Some(data.lastName),
          email     = Some(data.email),
          avatarUrl = None
        )
        for {
        //          avatar <- avatarService.retrieveURL(data.email)
          user      <- userService.simplyAdd(newUser) // TODO modify simplyAdd and generalise this type off call
          account   <- accountService.save(Account(loginInfo, user, None))
          _         <- authInfoRepository.add(loginInfo, authInfo)
        } yield {
          \/-(account)
        }
    }
  }.recover {
    case e: Exception => -\/(FailError(e))
  }
}
