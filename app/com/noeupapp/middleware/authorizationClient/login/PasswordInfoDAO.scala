package com.noeupapp.middleware.authorizationClient.login

import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.PasswordInfo
import org.sedis.Pool
import play.api.libs.json.Json
import com.noeupapp.middleware.authorizationClient.login.PasswordInfoDAO.passwordInfoFormat

object PasswordInfoDAO {
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
}

/**
  * The DAO to store the password information.
  */
class PasswordInfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[PasswordInfo]
