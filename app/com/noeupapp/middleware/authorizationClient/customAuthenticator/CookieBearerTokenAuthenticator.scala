package com.noeupapp.middleware.authorizationClient.customAuthenticator


import com.mohiva.play.silhouette._
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.exceptions._
import com.mohiva.play.silhouette.api.services.AuthenticatorService._
import com.mohiva.play.silhouette.api.services.{AuthenticatorResult, AuthenticatorService}
import com.mohiva.play.silhouette.api.util.{Base64, Clock, FingerprintGenerator, IDGenerator}
import com.mohiva.play.silhouette.api.{ExpirableAuthenticator, Logger, LoginInfo, StorableAuthenticator}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator.serialize
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticatorService.ID
import com.mohiva.play.silhouette.impl.daos.AuthenticatorDAO
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.util.JsonFormats._
import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.libs.Crypto
import play.api.libs.json.{JsResult, Json}
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticatorService._

/**
  * An authenticator that uses a header based approach with the help of a bearer token. It
  * works by transporting a token in a user defined header to track the authenticated user
  * and a server side backing store that maps the token to an authenticator instance.
  *
  * The authenticator can use sliding window expiration. This means that the authenticator times
  * out after a certain time if it wasn't used. This can be controlled with the [[idleTimeout]]
  * property.
  *
  * Note: If deploying to multiple nodes the backing store will need to synchronize.
  *
  * @param id The authenticator ID.
  * @param loginInfo The linked login info for an identity.
  * @param lastUsedDateTime The last used date/time.
  * @param expirationDateTime The expiration date/time.
  * @param idleTimeout The duration an authenticator can be idle before it timed out.
  */
case class CookieBearerTokenAuthenticator(
                                     id: String,
                                     loginInfo: LoginInfo,
                                     lastUsedDateTime: DateTime,
                                     expirationDateTime: DateTime,
                                     idleTimeout: Option[FiniteDuration],
                                     cookieMaxAge: Option[FiniteDuration],
                                     fingerprint: Option[String])
  extends StorableAuthenticator with ExpirableAuthenticator {

  /**
    * The Type of the generated value an authenticator will be serialized to.
    */
  override type Value = (String, Cookie)
}


object CookieBearerTokenAuthenticator {


  implicit val cookieBearerTokenAuthenticatorFormat = Json.format[CookieBearerTokenAuthenticator]

  implicit def string2CookieBearerTokenAuthenticator(serialised: String): JsResult[CookieBearerTokenAuthenticator] =
    Json.parse(serialised).validate[CookieBearerTokenAuthenticator]
  implicit def cookieBearerTokenAuthenticator2string(toSerialize: CookieBearerTokenAuthenticator): String =
    Json.stringify(Json.toJson(toSerialize))
}


/**
  * The companion object of the authenticator.
  */
object CookieAuthenticator extends Logger {

  /**
    * Converts the CookieAuthenticator to Json and vice versa.
    */
  implicit val jsonFormat = Json.format[CookieBearerTokenAuthenticator]

  /**
    * Serializes the authenticator.
    *
    * @param authenticator The authenticator to serialize.
    * @param settings The authenticator settings.
    * @return The serialized authenticator.
    */
  def serialize(authenticator: CookieBearerTokenAuthenticator)(settings: CookieBearerTokenAuthenticatorSettings): String = {
    if (settings.encryptAuthenticator) {
      Crypto.encryptAES(Json.toJson(authenticator).toString())
    } else {
      Base64.encode(Json.toJson(authenticator))
    }
  }

  /**
    * Unserializes the authenticator.
    *
    * @param str The string representation of the authenticator.
    * @param settings The authenticator settings.
    * @return Some authenticator on success, otherwise None.
    */
  def unserialize(str: String)(settings: CookieBearerTokenAuthenticatorSettings): Try[CookieBearerTokenAuthenticator] = {
    if (settings.encryptAuthenticator) buildAuthenticator(Crypto.decryptAES(str))
    else buildAuthenticator(Base64.decode(str))
  }

  /**
    * Builds the authenticator from Json.
    *
    * @param str The string representation of the authenticator.
    * @return Some authenticator on success, otherwise None.
    */
  private def buildAuthenticator(str: String): Try[CookieBearerTokenAuthenticator] = {
    Try(Json.parse(str)) match {
      case Success(json) => json.validate[CookieBearerTokenAuthenticator].asEither match {
        case Left(error) => Failure(new AuthenticatorException(InvalidJsonFormat.format(ID, error)))
        case Right(authenticator) => Success(authenticator)
      }
      case Failure(error) => Failure(new AuthenticatorException(JsonParseError.format(ID, str), error))
    }
  }
}




/**
  * The service that handles the bearer token authenticator.
  *
  * @param settings The authenticator settings.
  * @param dao The DAO to store the authenticator.
  * @param idGenerator The ID generator used to create the authenticator ID.
  * @param clock The clock implementation.
  * @param executionContext The execution context to handle the asynchronous operations.
  */
class CookieBearerTokenAuthenticatorService(
                                       settings: CookieBearerTokenAuthenticatorSettings,
                                       dao: AuthenticatorDAO[CookieBearerTokenAuthenticator],
                                       fingerprintGenerator: FingerprintGenerator,
                                       idGenerator: IDGenerator,
                                       clock: Clock)(implicit val executionContext: ExecutionContext)
  extends AuthenticatorService[CookieBearerTokenAuthenticator]
    with Logger {

  import CookieAuthenticator._

  /**
    * Creates a new authenticator for the specified login info.
    *
    * @param loginInfo The login info for which the authenticator should be created.
    * @param request The request header.
    * @return An authenticator.
    */
  override def create(loginInfo: LoginInfo)(implicit request: RequestHeader): Future[CookieBearerTokenAuthenticator] = {
    idGenerator.generate.map { id =>
      val now = clock.now
      CookieBearerTokenAuthenticator(
        id = id,
        loginInfo = loginInfo,
        lastUsedDateTime = now,
        expirationDateTime = now + settings.authenticatorExpiry,
        idleTimeout = settings.authenticatorIdleTimeout,
        cookieMaxAge = settings.cookieMaxAge,
        fingerprint = if (settings.useFingerprinting) Some(fingerprintGenerator.generate) else None)
    }.recover {
      case e => throw new AuthenticatorCreationException(CreateError.format(ID, loginInfo), e)
    }
  }

  /**
    * Retrieves the authenticator from request.
    *
    * @param request The request header.
    * @return Some authenticator or None if no authenticator could be found in request.
    */
  override def retrieve(implicit request: RequestHeader): Future[Option[CookieBearerTokenAuthenticator]] =
    retrieveBearerToken.flatMap{
      case cbtauth @ Some(_) => Future.successful(cbtauth)
      case None => retrieveCookie
    }


  private def retrieveBearerToken(implicit request: RequestHeader): Future[Option[CookieBearerTokenAuthenticator]] = {
    Future.from(Try(request.headers.get(settings.headerName))).flatMap {
      case Some(token) => dao.find(token)
      case None => Future.successful(None)
    }.recover {
      case e => throw new AuthenticatorRetrievalException(RetrieveError.format(ID), e)
    }
  }


  private def retrieveCookie(implicit request: RequestHeader): Future[Option[CookieBearerTokenAuthenticator]] = {
    Future.from(Try {
      if (settings.useFingerprinting) Some(fingerprintGenerator.generate) else None
    }).flatMap { fingerprint =>
      request.cookies.get(settings.cookieName) match {
        case Some(cookie) =>
          (unserialize(cookie.value)(settings) match {
            case Success(authenticator) => Future.successful(Some(authenticator))
            case Failure(error) =>
              logger.info(error.getMessage, error)
              Future.successful(None)
          }).map {
            case Some(a) if fingerprint.isDefined && a.fingerprint != fingerprint =>
              logger.info(InvalidFingerprint.format(ID, fingerprint, a))
              None
            case v => v
          }
        case None => Future.successful(None)
      }
    }.recover {
      case e => throw new AuthenticatorRetrievalException(RetrieveError.format(ID), e)
    }
  }

  /**
    * Creates a new bearer token for the given authenticator and return it. The authenticator will also be
    * stored in the backing store.
    *
    * @param authenticator The authenticator instance.
    * @param request The request header.
    * @return The serialized authenticator value.
    */
  override def init(authenticator: CookieBearerTokenAuthenticator)(implicit request: RequestHeader): Future[(String, Cookie)] = {
    dao.add(authenticator).map { cdtauth =>
      val cookie =
        Cookie(
          name = settings.cookieName,
          value = CookieAuthenticator.serialize(authenticator)(settings),
          // The maxAge` must be used from the authenticator, because it might be changed by the user
          // to implement "Remember Me" functionality
          maxAge = authenticator.cookieMaxAge.map(_.toSeconds.toInt),
          path = settings.cookiePath,
          domain = settings.cookieDomain,
          secure = settings.secureCookie,
          httpOnly = settings.httpOnlyCookie
        )
      (cdtauth.id, cookie)
    }.recover {
      case e => throw new AuthenticatorInitializationException(InitError.format(ID, authenticator), e)
    }
  }


  /**
    * Adds a header with the token as value to the result.
    *
    * @param token The token to embed.
    * @param result The result to manipulate.
    * @param request The request header.
    * @return The manipulated result.
    */
  override def embed(tokenCookie: (String, Cookie), result: Result)(implicit request: RequestHeader): Future[AuthenticatorResult] = {
    val token: (Result) => Result = embedBearerTokenResult(tokenCookie._1, _)(request)
    val cookie: (Result) => Result = embedCookieResult(tokenCookie._2, _)(request)
    val transformedResult = (token andThen cookie)(result)
    Future.successful(AuthenticatorResult(transformedResult))
  }

  private def embedBearerTokenResult(token: String, result: Result)(implicit request: RequestHeader): Result = result.withHeaders(settings.headerName -> token)

  private def embedCookieResult(cookie: Cookie, result: Result)(implicit request: RequestHeader): Result = AuthenticatorResult(result.withCookies(cookie))

  /**
    * Adds a header with the token as value to the request.
    *
    * @param token The token to embed.
    * @param request The request header.
    * @return The manipulated request header.
    */
  override def embed(tokenCookie: (String, Cookie), request: RequestHeader): RequestHeader = {
    val tokenRequestHeader: (RequestHeader) => RequestHeader =
      embedBearerTokenRequestHeader(tokenCookie._1, _)

    val cookieRequestHeader: (RequestHeader) => RequestHeader =
      embedCookieRequestHeader(tokenCookie._2, _)

    (tokenRequestHeader andThen cookieRequestHeader)(request)
  }

  private def embedBearerTokenRequestHeader(token: String, request: RequestHeader): RequestHeader = {
    val additional = Seq(settings.headerName -> token)
    request.copy(headers = request.headers.replace(additional: _*))
  }

  private def embedCookieRequestHeader(cookie: Cookie, request: RequestHeader): RequestHeader = {
    val cookies = Cookies.mergeCookieHeader(request.headers.get(HeaderNames.COOKIE).getOrElse(""), Seq(cookie))
    val additional = Seq(HeaderNames.COOKIE -> cookies)
    request.copy(headers = request.headers.replace(additional: _*))
  }

  /**
    * @inheritdoc
    *
    * @param authenticator The authenticator to touch.
    * @return The touched authenticator on the left or the untouched authenticator on the right.
    */
  override def touch(authenticator: CookieBearerTokenAuthenticator): Either[CookieBearerTokenAuthenticator, CookieBearerTokenAuthenticator] = {
    if (authenticator.idleTimeout.isDefined) {
      Left(authenticator.copy(lastUsedDateTime = clock.now))
    } else {
      Right(authenticator)
    }
  }

  /**
    * Updates the authenticator with the new last used date in the backing store.
    *
    * We needn't embed the token in the response here because the token itself will not be changed.
    * Only the authenticator in the backing store will be changed.
    *
    * @param authenticator The authenticator to update.
    * @param result The result to manipulate.
    * @param request The request header.
    * @return The original or a manipulated result.
    */
  override def update(authenticator: CookieBearerTokenAuthenticator, result: Result)(
    implicit request: RequestHeader): Future[AuthenticatorResult] = {

    dao.update(authenticator).map { a =>
      AuthenticatorResult(result.withCookies(Cookie(
        name = settings.cookieName,
        value = CookieAuthenticator.serialize(authenticator)(settings),
        // The maxAge` must be used from the authenticator, because it might be changed by the user
        // to implement "Remember Me" functionality
        maxAge = authenticator.cookieMaxAge.map(_.toSeconds.toInt),
        path = settings.cookiePath,
        domain = settings.cookieDomain,
        secure = settings.secureCookie,
        httpOnly = settings.httpOnlyCookie
      )))
    }.recover {
      case e => throw new AuthenticatorUpdateException(UpdateError.format(ID, authenticator), e)
    }
  }

  /**
    * Renews an authenticator.
    *
    * After that it isn't possible to use a bearer token which was bound to this authenticator. This
    * method doesn't embed the the authenticator into the result. This must be done manually if needed
    * or use the other renew method otherwise.
    *
    * @param authenticator The authenticator to renew.
    * @param request The request header.
    * @return The serialized expression of the authenticator.
    */
  override def renew(authenticator: CookieBearerTokenAuthenticator)(
    implicit request: RequestHeader): Future[(String, Cookie)] = {

    dao.remove(authenticator.id).flatMap { _ =>
      create(authenticator.loginInfo).flatMap(init)
    }.recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), e)
    }
  }

  /**
    * Renews an authenticator and replaces the bearer token header with a new one.
    *
    * The old authenticator will be revoked. After that it isn't possible to use a bearer token which was
    * bound to this authenticator.
    *
    * @param authenticator The authenticator to update.
    * @param result The result to manipulate.
    * @param request The request header.
    * @return The original or a manipulated result.
    */
  override def renew(authenticator: CookieBearerTokenAuthenticator, result: Result)(
    implicit request: RequestHeader): Future[AuthenticatorResult] = {

    renew(authenticator).flatMap(v => embed(v, result)).recover {
      case e => throw new AuthenticatorRenewalException(RenewError.format(ID, authenticator), e)
    }
  }

  /**
    * Removes the authenticator from cache.
    *
    * @param result The result to manipulate.
    * @param request The request header.
    * @return The manipulated result.
    */
  override def discard(authenticator: CookieBearerTokenAuthenticator, result: Result)(
    implicit request: RequestHeader): Future[AuthenticatorResult] = {

    dao.remove(authenticator.id).map { _ =>
      AuthenticatorResult(result.discardingCookies(DiscardingCookie(
        name = settings.cookieName,
        path = settings.cookiePath,
        domain = settings.cookieDomain,
        secure = settings.secureCookie)))
    }.recover {
      case e => throw new AuthenticatorDiscardingException(DiscardError.format(ID, authenticator), e)
    }
  }
}

/**
  * The companion object of the authenticator service.
  */
object CookieBearerTokenAuthenticatorService {

  /**
    * The ID of the authenticator.
    */
  val ID = "cookie-bearer-token-authenticator"


  /**
    * The error messages.
    */
  val JsonParseError = "[Silhouette][%s] Cannot parse Json: %s"
  val InvalidJsonFormat = "[Silhouette][%s] Invalid Json format: %s"
  val InvalidFingerprint = "[Silhouette][%s] Fingerprint %s doesn't match authenticator: %s"

}

/**
  * The settings for the bearer token authenticator.
  *
  * @param headerName The name of the header in which the token will be transferred.
  * @param authenticatorIdleTimeout The duration an authenticator can be idle before it timed out.
  * @param authenticatorExpiry The duration an authenticator expires after it was created.
  */
case class CookieBearerTokenAuthenticatorSettings(cookieName: String = "id",
                                                  cookiePath: String = "/",
                                                  cookieDomain: Option[String] = None,
                                                  secureCookie: Boolean = true,
                                                  httpOnlyCookie: Boolean = true,
                                                  encryptAuthenticator: Boolean = true,
                                                  useFingerprinting: Boolean = true,
                                                  cookieMaxAge: Option[FiniteDuration] = None,
                                                  headerName: String = "X-Auth-Token",
                                                  authenticatorIdleTimeout: Option[FiniteDuration] = None,
                                                  authenticatorExpiry: FiniteDuration = 12 hours)
