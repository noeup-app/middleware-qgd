package com.noeupapp.middleware.entities.user.email

import com.noeupapp.middleware.entities.user.User

/**
  * UpdateEmail email factory
  */
trait UpdateEmailEmailFactory {

  def getUpdateMailRequestEmail(user: User, token: String, newEmail: String): (String, String)
  def getConfirmChangedEmail(user: User, newEmail: String): (String, String)

}
