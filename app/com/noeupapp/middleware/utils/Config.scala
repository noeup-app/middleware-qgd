package com.noeupapp.middleware.utils

import com.typesafe.config.ConfigFactory
//import com.yetu.oauth2provider.data.riak.RiakConnection
import org.joda.time.DateTimeConstants.MILLIS_PER_SECOND
import play.api.Play
import play.api.Play.current

object Config {

  // TODO : .get ??? Error !

  lazy val minimumStateLength = Play.configuration.getInt("authorize.state.minLength").get
  lazy val maximumStateLength = Play.configuration.getInt("authorize.state.maxLength").get

  /**
   * Returns the default redirect url for current stage
   */
  lazy val redirectAfterLogin = Play.configuration.getString("redirect.afterlogin").get

  case class GoogleAnalytics(enabled: Boolean, trackingId: String)

  lazy val googleAnalytics = GoogleAnalytics(Play.configuration.getBoolean("webanalytics.google.enabled").get, Play.configuration.getString("webanalytics.google.trackingId").get)

  lazy val redirectURICheckingEnabled = Play.configuration.getBoolean("security.redirectURICheckingEnabled").get

  // TODO : REMOVE
  // OAUTH2 constants
  val GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code"
  val GRANT_TYPE_RESOURCE_OWNER_PASSWORD = "password"
  val GRANT_TYPE_SIGNATURE = "signature"
  val GRANT_TYPE_TOKEN = "token"
  val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"

  //OAuth scopes
  val SCOPE_ID = "id"
  val SCOPE_BASIC = "basic"
  val SCOPE_EVENTS = "events"
  val SCOPE_CONTACT = "contact"
  val SCOPE_PASSWORD = "password"
  val SCOPE_REGISTRATION_INFO = "registrationInfo"
  val SCOPE_HOUSEHOLD_READ = "householdRead"
  val SCOPE_HOUSEHOLD_WRITE = "householdWrite"
  val SCOPE_HOUSEHOLD_GENERATE = "householdGenerate"

  /**
   * combination of REGISTRATION_INFO and HOUSEHOLD_READ
   * This scope is redundant once we can allow more than one scope on the query string
   * (requires change in the nulab third-party library)
   */
  val SCOPE_CONTROLCENTER = "controlcenter"

  object OAuth2 {

    object Defaults {
      val defaultGrantTypes = Some(List(GRANT_TYPE_AUTHORIZATION_CODE, GRANT_TYPE_RESOURCE_OWNER_PASSWORD, GRANT_TYPE_SIGNATURE, GRANT_TYPE_TOKEN))
      // TODO move to GrantType
    }

    lazy val jsonWebTokenPrivateKeyFilename = Play.configuration.getString("security.jsonWebToken.privateKeyFilename").get
    lazy val jsonWebTokenPublicKeyFilename = Play.configuration.getString("security.jsonWebToken.publicKeyFilename").get

    lazy val accessTokenExpirationInSeconds: Long = Play.configuration.getLong("security.expireTimes.accessTokenInSeconds").getOrElse((60 * 60).toLong)

    lazy val signatureDateExpirationInMilliseconds = Play.configuration.getLong("security.expireTimes.signatureInSeconds").get * MILLIS_PER_SECOND

    lazy val authTokenLength: Int = Play.configuration.getInt("security.authCode.length").get

  }


  // play.configuration requires a started play app; however this configuration value needs to eb read before
  // application start. Use the standard ConfigFactory (loading reference.conf / middleware.conf)
  val config = ConfigFactory.load()
  val persist = config.getBoolean("persist")

}