package com.noeupapp.middleware.authorizationClient.customAuthenticator

import com.google.inject.Inject
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticatorDAO.CookieBearerTokenAuthenticatorKey
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.entities.user.UserService
import org.sedis.Pool
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class CookieBearerTokenAuthenticatorDAO @Inject() (settings: CookieBearerTokenAuthenticatorSettings,
                                        authAccessTokenService: OAuthAccessTokenService,
                                        pool: Pool
                            ) extends AuthenticatorDAO[CookieBearerTokenAuthenticator]{

  override def find(id: String): Future[Option[CookieBearerTokenAuthenticator]] = {

    val res: Option[JsResult[CookieBearerTokenAuthenticator]] =
      pool.withClient(_.get(CookieBearerTokenAuthenticatorKey(id))).map(e => e)

    res match {
      case Some(JsSuccess(value, _)) => Future.successful(Some(value))
      case Some(JsError(errors)) =>
        Logger.error(s"CookieBearerTokenAuthenticatorDAO - Unable to validate json format : $errors, removing it...")
        remove(id).map(_ => None)
      case None => Future.successful(None)
    }

  }

  override def update(authenticator: CookieBearerTokenAuthenticator): Future[CookieBearerTokenAuthenticator] =
    add(authenticator) // add == update in redis

  override def remove(id: String): Future[Unit] = {

    val key: String = CookieBearerTokenAuthenticatorKey(id)

    pool.withClient(_.del(key))
    Future.successful(())
  }

  override def add(authenticator: CookieBearerTokenAuthenticator): Future[CookieBearerTokenAuthenticator] = {

    val key: String = CookieBearerTokenAuthenticatorKey(authenticator.id)
    val value: String = authenticator
    pool.withClient(_.set(key, value))
    pool.withClient(_.expireAt(key, authenticator.expirationDateTime.getMillis / 1000))

    Future.successful(authenticator)
  }
}


object CookieBearerTokenAuthenticatorDAO {

  case class CookieBearerTokenAuthenticatorKey(cookieBearerTokenAuthenticatorId: String)

  object CookieBearerTokenAuthenticatorKey{
    implicit def cookieBearerTokenAuthenticatorKey2String(o: CookieBearerTokenAuthenticatorKey): String = Json.stringify(Json.toJson(o))
    implicit def string2CookieBearerTokenAuthenticatorKey(serialised: String): JsResult[CookieBearerTokenAuthenticatorKey] =
      Json.parse(serialised).validate[CookieBearerTokenAuthenticatorKey]
  }

  implicit val cookieBearerTokenAuthenticatorFormat = Json.format[CookieBearerTokenAuthenticatorKey]
}