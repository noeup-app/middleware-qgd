package qgd.authorizationClient.models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.{OAuth1Info, OAuth2Info}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Future

import qgd.authorizationClient.models.daos.OAuth2InfoDAO.oAuth2InfoFormat

object OAuth2InfoDAO {
  implicit val oAuth2InfoFormat = Json.format[OAuth2Info]
}

/**
  * The DAO to store the OAuth2 information.
  */
class OAuth2InfoDAO extends AuthInfoDAOImpl[OAuth2Info]
