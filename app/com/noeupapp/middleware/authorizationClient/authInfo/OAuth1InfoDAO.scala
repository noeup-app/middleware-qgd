package com.noeupapp.middleware.authorizationClient.authInfo

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import org.sedis.Pool
import play.api.libs.json.Json
import com.noeupapp.middleware.authorizationClient.authInfo.OAuth1InfoDAO.oAuth1InfoFormat

object OAuth1InfoDAO {
  implicit val oAuth1InfoFormat = Json.format[OAuth1Info]
}

/**
  * The DAO to store the OAuth1 information.
  */
class OAuth1InfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[OAuth1Info]
