package com.noeupapp.middleware.authorizationClient.authInfo

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import org.sedis.Pool
import play.api.libs.json.Json
import com.noeupapp.middleware.authorizationClient.authInfo.OAuth2InfoDAO.oAuth2InfoFormat


object OAuth2InfoDAO {
  implicit val oAuth2InfoFormat = Json.format[OAuth2Info]
}

/**
  * The DAO to store the OAuth2 information.
  */
class OAuth2InfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[OAuth2Info]
