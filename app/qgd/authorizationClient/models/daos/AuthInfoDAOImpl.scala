package qgd.authorizationClient.models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{LoginInfo, AuthInfo}
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import org.sedis.Pool
import play.api.libs.json.{Writes, Reads}
import qgd.authorizationClient.utils.CaseClassUtils
import redis.clients.jedis.{JedisPool, JedisPoolConfig}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.reflect.ClassTag

class AuthInfoDAOImpl[T <: AuthInfo](implicit writes: Writes[T], reads: Reads[T], override val classTag: ClassTag[T]) extends DelegableAuthInfoDAO[T] with CaseClassUtils {

  val pool = new Pool(new JedisPool(new JedisPoolConfig(), "localhost", 6379, 2000))

  /**
    * Finds the auth info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
    */
  override def find(loginInfo: LoginInfo): Future[Option[T]] =
    Future.successful(pool.withClient(_.get(loginInfo)) flatMap (r => r))

  /**
    * Updates the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be updated.
    * @param authInfo The auth info to update.
    * @return The updated auth info.
    */
  override def update(loginInfo: LoginInfo, authInfo: T): Future[T] =
    add(loginInfo, authInfo)

  /**
    * Removes the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be removed.
    * @return A future to wait for the process to be completed.
    */
  override def remove(loginInfo: LoginInfo): Future[Unit] ={
    val serializedLoginInfo: String = loginInfo
    Future.successful(pool.withClient(_.del(serializedLoginInfo)))
  }

  /**
    * Saves the auth info for the given login info.
    *
    * This method either adds the auth info if it doesn't exists or it updates the auth info
    * if it already exists.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo The auth info to save.
    * @return The saved auth info.
    */
  override def save(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  /**
    * Adds new auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be added.
    * @param authInfo The auth info to add.
    * @return The added auth info.
    */
  override def add(loginInfo: LoginInfo, authInfo: T): Future[T] = {
    val serializedLoginInfo: String = loginInfo
    val serializedAuthInfo: String  = authInfo
    pool.withClient(_.set(serializedLoginInfo, serializedAuthInfo))
    Future.successful(authInfo)
  }
}
