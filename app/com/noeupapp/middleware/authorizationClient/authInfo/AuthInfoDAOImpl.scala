package com.noeupapp.middleware.authorizationClient.authInfo

import com.mohiva.play.silhouette.api
import com.mohiva.play.silhouette.api.AuthInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.CaseClassUtils
import org.sedis.Pool
import play.api.Logger
import play.api.libs.json.{Reads, Writes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scalaz.{-\/, \/-}

abstract class AuthInfoDAOImpl[T <: AuthInfo](implicit writes: Writes[T], reads: Reads[T], override val classTag: ClassTag[T]) extends DelegableAuthInfoDAO[T] with CaseClassUtils {

  val pool: Pool

  /**
    * Finds the auth info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
    */
  override def find(loginInfo: api.LoginInfo): Future[Option[T]] = Future {
    Logger.debug(s"AuthInfoDAOImpl.find($loginInfo)")
    Try{
      pool.withClient(_.get(loginInfo)) flatMap (r => r)
    } match {
      case \/-(res) => res
      case -\/(e) =>
        Logger.error("AuthInfoDAOImpl.find" + e.toString)
        None
    }
  }

  /**
    * Removes the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be removed.
    * @return A future to wait for the process to be completed.
    */
  override def remove(loginInfo: api.LoginInfo): Future[Unit] = Future {
    Logger.error(s"AuthInfoDAOImpl.remove($loginInfo)")
    val serializedLoginInfo: String = loginInfo
    Try {
      pool.withClient(_.del(serializedLoginInfo))
    } match {
      case \/-(_) =>
      case -\/(e) =>
        Logger.error("AuthInfoDAOImpl.find" + e.toString)
    }
  }

  /**
    * Saves the auth info for the given login info.
    *
    * This method either adds the auth info if it doesn't exists or it updates the auth info
    * if it already exists.
    *
    * NOTE : in our implementation (with redis database) add = update. So, save, add and update could be merged. But the library requires this 3 methods.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo The auth info to save.
    * @return The saved auth info.
    */
  override def save(loginInfo: api.LoginInfo, authInfo: T): Future[T] = {
    Logger.error(s"AuthInfoDAOImpl.save($loginInfo)")
    find(loginInfo).flatMap { // TODO manage errors
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
  override def add(loginInfo: api.LoginInfo, authInfo: T): Future[T] = {
    val serializedLoginInfo: String = loginInfo
    val serializedAuthInfo: String  = authInfo
    try{
      pool.withClient(_.set(serializedLoginInfo, serializedAuthInfo)) // TODO manage errors
    }catch {
      case e: Exception => Logger.error("AuthInfoDAOImpl.add", e)
    }
    Future.successful(authInfo)
  }


  /**
    * Updates the auth info for the given login info.
    *
    * @param loginInfo The login info for which the auth info should be updated.
    * @param authInfo The auth info to update.
    * @return The updated auth info.
    */
  override def update(loginInfo: api.LoginInfo, authInfo: T): Future[T] =
    add(loginInfo, authInfo)

}
