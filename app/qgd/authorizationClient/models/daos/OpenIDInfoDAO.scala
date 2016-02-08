package qgd.authorizationClient.models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Future

import qgd.authorizationClient.models.daos.OpenIDInfoDAO.openIDInfoFormat

object OpenIDInfoDAO {
  implicit val openIDInfoFormat = Json.format[OpenIDInfo]
}

/**
  * The DAO to store the OpenID information.
  */
class OpenIDInfoDAO extends AuthInfoDAOImpl[OpenIDInfo]

