package qgd.authorizationClient.models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import org.sedis.Pool
import play.api.Logger
import play.api.libs.json.{Writes, JsValue, Json}
import play.api.libs.concurrent.Execution.Implicits._

import scala.collection.mutable
import scala.concurrent.Future

import qgd.authorizationClient.models.daos.PasswordInfoDAO.passwordInfoFormat


object PasswordInfoDAO {
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
}

/**
  * The DAO to store the password information.
  */
class PasswordInfoDAO @Inject() (val pool: Pool) extends AuthInfoDAOImpl[PasswordInfo]
