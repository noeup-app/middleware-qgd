package qgd.authorizationClient.models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import org.sedis.Pool
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.Future

import qgd.authorizationClient.models.daos.OAuth1InfoDAO.oAuth1InfoFormat

object OAuth1InfoDAO {
  implicit val oAuth1InfoFormat = Json.format[OAuth1Info]
}

/**
  * The DAO to store the OAuth1 information.
  */
class OAuth1InfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[OAuth1Info]
