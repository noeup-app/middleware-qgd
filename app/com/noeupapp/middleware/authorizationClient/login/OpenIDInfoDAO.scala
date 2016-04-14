package com.noeupapp.middleware.authorizationClient.login

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import org.sedis.Pool
import play.api.libs.json.Json
import com.noeupapp.middleware.authorizationClient.login.OpenIDInfoDAO.openIDInfoFormat

object OpenIDInfoDAO {
  implicit val openIDInfoFormat = Json.format[OpenIDInfo]
}

/**
  * The DAO to store the OpenID information.
  */
class OpenIDInfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[OpenIDInfo]

