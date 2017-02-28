package com.noeupapp.middleware.packages

import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.Future

trait PackageHandler {

  def isAuthorized(user: User, actionName: String): Future[Expect[Unit]]

}
