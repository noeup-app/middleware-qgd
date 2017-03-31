package com.noeupapp.middleware

import com.amazonaws.{ClientConfiguration, Protocol}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{CookieSecretProvider, CookieSecretSettings}
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.oauth2.state.{CookieStateProvider, CookieStateSettings}
import com.mohiva.play.silhouette.impl.repositories.DelegableAuthInfoRepository
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.noeupapp.middleware.authorizationClient.confirmEmail.ConfirmEmailConfig
import com.noeupapp.middleware.authorizationClient.customAuthenticator.{CookieBearerTokenAuthenticator, CookieBearerTokenAuthenticatorDAO, CookieBearerTokenAuthenticatorService, CookieBearerTokenAuthenticatorSettings}
import com.noeupapp.middleware.authorizationClient.login._
import com.noeupapp.middleware.authorizationClient.{ScopeAndRoleAuthorization, ScopeAndRoleAuthorizationImpl}
import com.noeupapp.middleware.entities.user.UserService
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import com.noeupapp.middleware.authorizationClient.provider.QGDProvider
import com.noeupapp.middleware.authorizationClient.forgotPassword.ForgotPasswordConfig
import com.noeupapp.middleware.authorizationClient.authenticator.BearerAuthenticatorDAO
import com.noeupapp.middleware.authorizationClient.authInfo.{OAuth1InfoDAO, OAuth2InfoDAO, OpenIDInfoDAO, PasswordInfoDAO}
import com.noeupapp.middleware.authorizationServer.oauthAccessToken.OAuthAccessTokenService
import com.noeupapp.middleware.config.AppConfig
import com.noeupapp.middleware.entities.account.{Account, AccountService}
import com.noeupapp.middleware.oauth2.TierAccessTokenConfig
import com.noeupapp.middleware.utils.{Html2PdfConfig, SendinBlueConfig}
import com.noeupapp.middleware.utils.s3.{AmazonS3CoweboClient, S3Config, S3CoweboConfig}
import org.sedis.Pool


/**
 * The Guice module which wires all Silhouette dependencies.
 */
class MiddlewareGuiceModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure() {
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDAO]
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDAO]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO]
    bind[DelegableAuthInfoDAO[OpenIDInfo]].to[OpenIDInfoDAO]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
    bind[ScopeAndRoleAuthorization].to[ScopeAndRoleAuthorizationImpl]
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideBearerTokenEnvironment(
                          accountService: AccountService,
                          authenticatorService: AuthenticatorService[BearerTokenAuthenticator],
                          eventBus: EventBus): Environment[Account, BearerTokenAuthenticator] = {

    Environment[Account, BearerTokenAuthenticator](
      accountService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideCookieBearerTokenEnvironment(
                          accountService: AccountService,
                          authenticatorService: AuthenticatorService[CookieBearerTokenAuthenticator],
                          eventBus: EventBus): Environment[Account, CookieBearerTokenAuthenticator] = {

    Environment[Account, CookieBearerTokenAuthenticator](
      accountService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }


  /**
   * Provides the social provider registry.
   *
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(googleProvider: GoogleProvider, qgdProvider: QGDProvider): SocialProviderRegistry = {
    SocialProviderRegistry(Seq(googleProvider, qgdProvider))
  }

  /**
   * Provides the authenticator service.
   *
   * @param fingerprintGenerator The fingerprint generator implementation.
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideCookieBearerTokenAuthenticatorService(pool: Pool,
                                                    idGenerator: IDGenerator,
                                                    cacheLayer: CacheLayer,
                                                    fingerprintGenerator: FingerprintGenerator,
                                                    //                                 dao: BearerAuthenticatorDAO,
                                                    accessTokenService: OAuthAccessTokenService,
                                                    userService: UserService,
                                                    configuration: Configuration,
                                                    clock: Clock): AuthenticatorService[CookieBearerTokenAuthenticator] = {


    val config = configuration.underlying.as[CookieBearerTokenAuthenticatorSettings]("silhouette.authenticator")
    val dao = new CookieBearerTokenAuthenticatorDAO(config, accessTokenService, userService, pool)
    //    val config: BearerTokenAuthenticatorSettings = BearerTokenAuthenticatorSettings()
    new CookieBearerTokenAuthenticatorService(config, dao, fingerprintGenerator, idGenerator, clock)
  }


  @Provides
  def provideCookieBearerTokenAuthenticatorSettings(configuration: Configuration): CookieBearerTokenAuthenticatorSettings =
    configuration.underlying.as[CookieBearerTokenAuthenticatorSettings]("silhouette.authenticator")

  @Provides
  def provideBearerTokenAuthenticatorService(pool: Pool,
                                                    idGenerator: IDGenerator,
                                                    cacheLayer: CacheLayer,
                                                    fingerprintGenerator: FingerprintGenerator,
                                                    //                                 dao: BearerAuthenticatorDAO,
                                                    accessTokenService: OAuthAccessTokenService,
                                                    userService: UserService,
                                                    configuration: Configuration,
                                                    clock: Clock): AuthenticatorService[BearerTokenAuthenticator] = {

    val config = configuration.underlying.as[BearerTokenAuthenticatorSettings]("silhouette.authenticator")
    val dao = new BearerAuthenticatorDAO(accessTokenService, userService)
    //    val config: BearerTokenAuthenticatorSettings = BearerTokenAuthenticatorSettings()
    new BearerTokenAuthenticatorService(config, dao, idGenerator, clock)
  }


  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
   * @param openIDInfoDAO The implementation of the delegable OpenID auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
    oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]): AuthInfoRepository = {

    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
   * Provides the OAuth1 token secret provider.
   *
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth1 token secret provider implementation.
   */
  @Provides
  def provideOAuth1TokenSecretProvider(configuration: Configuration, clock: Clock): OAuth1TokenSecretProvider = {
    val settings = configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider")
    new CookieSecretProvider(settings, clock)
  }

  /**
   * Provides the OAuth2 state provider.
   *
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth2 state provider implementation.
   */
  @Provides
  def provideOAuth2StateProvider(idGenerator: IDGenerator, configuration: Configuration, clock: Clock): OAuth2StateProvider = {
    val settings = configuration.underlying.as[CookieStateSettings]("silhouette.oauth2StateProvider")
    new CookieStateProvider(settings, idGenerator, clock)
  }

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasher The default password hasher implementation.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
    authInfoRepository: AuthInfoRepository,
    passwordHasher: PasswordHasher): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasher, Seq(passwordHasher))
  }

  @Provides
  def provideTierAccessTokenConfig(configuration: Configuration): TierAccessTokenConfig = {
    configuration.underlying.as[TierAccessTokenConfig]("tier.accesstoken")
  }

//  /**
//   * Provides the Facebook provider.
//   *
//   * @param httpLayer The HTTP layer implementation.
//   * @param stateProvider The OAuth2 state provider implementation.
//   * @param configuration The Play configuration.
//   * @return The Facebook provider.
//   */
//  @Provides
//  def provideFacebookProvider(
//    httpLayer: HTTPLayer,
//    stateProvider: OAuth2StateProvider,
//    configuration: Configuration): FacebookProvider = {
//
//    new FacebookProvider(httpLayer, stateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.facebook"))
//  }
//
  /**
   * Provides the Google provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration): GoogleProvider = {

    new GoogleProvider(httpLayer, stateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.google"))
  }

  /**
   * Provides **THE** QGD provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param stateProvider The OAuth2 state provider implementation.
   * @param configuration The Play configuration.
   * @return The QGD provider.
   */
  @Provides
  def provideQGDProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration,
    accessTokenService: OAuthAccessTokenService,
    userService: UserService): QGDProvider = {

    new QGDProvider(httpLayer, stateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.qgd"),accessTokenService, userService)
  }


//
//  /**
//   * Provides the VK provider.
//   *
//   * @param httpLayer The HTTP layer implementation.
//   * @param stateProvider The OAuth2 state provider implementation.
//   * @param configuration The Play configuration.
//   * @return The VK provider.
//   */
//  @Provides
//  def provideVKProvider(
//    httpLayer: HTTPLayer,
//    stateProvider: OAuth2StateProvider,
//    configuration: Configuration): VKProvider = {
//
//    new VKProvider(httpLayer, stateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.vk"))
//  }
//
//  /**
//   * Provides the Clef provider.
//   *
//   * @param httpLayer The HTTP layer implementation.
//   * @param configuration The Play configuration.
//   * @return The Clef provider.
//   */
//  @Provides
//  def provideClefProvider(httpLayer: HTTPLayer, configuration: Configuration): ClefProvider = {
//
//    new ClefProvider(httpLayer, new DummyStateProvider, configuration.underlying.as[OAuth2Settings]("silhouette.clef"))
//  }
//
//  /**
//   * Provides the Twitter provider.
//   *
//   * @param httpLayer The HTTP layer implementation.
//   * @param tokenSecretProvider The token secret provider implementation.
//   * @param configuration The Play configuration.
//   * @return The Twitter provider.
//   */
//  @Provides
//  def provideTwitterProvider(
//    httpLayer: HTTPLayer,
//    tokenSecretProvider: OAuth1TokenSecretProvider,
//    configuration: Configuration): TwitterProvider = {
//
//    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")
//    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
//  }
//
//  /**
//   * Provides the Xing provider.
//   *
//   * @param httpLayer The HTTP layer implementation.
//   * @param tokenSecretProvider The token secret provider implementation.
//   * @param configuration The Play configuration.
//   * @return The Xing provider.
//   */
//  @Provides
//  def provideXingProvider(
//    httpLayer: HTTPLayer,
//    tokenSecretProvider: OAuth1TokenSecretProvider,
//    configuration: Configuration): XingProvider = {
//
//    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.xing")
//    new XingProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
//  }
//
//  /**
//   * Provides the Yahoo provider.
//   *
//   * @param cacheLayer The cache layer implementation.
//   * @param httpLayer The HTTP layer implementation.
//   * @param client The OpenID client implementation.
//   * @param configuration The Play configuration.
//   * @return The Yahoo provider.
//   */
//  @Provides
//  def provideYahooProvider(
//    cacheLayer: CacheLayer,
//    httpLayer: HTTPLayer,
//    client: OpenIdClient,
//    configuration: Configuration): YahooProvider = {
//
//    val settings = configuration.underlying.as[OpenIDSettings]("silhouette.yahoo")
//    new YahooProvider(httpLayer, new PlayOpenIDService(client, settings), settings)
//  }



  @Provides
  def provideForgotPasswordConfig(configuration: Configuration): ForgotPasswordConfig = {
    configuration.underlying.as[ForgotPasswordConfig]("forgotPasswordConfig")
  }

  @Provides
  def provideConfirmEmailConfig(configuration: Configuration): ConfirmEmailConfig = {
    configuration.underlying.as[ConfirmEmailConfig]("confirmEmailConfig")
  }


  @Provides
  def provideS3Config(configuration: Configuration): S3Config = {
    configuration.underlying.as[S3Config]("s3config")
  }

  @Provides
  def provideS3CoweboConfig(configuration: Configuration): S3CoweboConfig = {
    configuration.underlying.as[S3CoweboConfig]("s3CoweboConfig")
  }

  @Provides
  def provideAmazonS3CoweboClient(s3CoweboConfig: S3CoweboConfig): AmazonS3CoweboClient = {
    val awsCredentials = new BasicAWSCredentials(s3CoweboConfig.key, s3CoweboConfig.secret)
    val config = new ClientConfiguration
    config.setSignerOverride("S3SignerType")
    val s3 = new AmazonS3CoweboClient(awsCredentials, config.withProtocol(Protocol.HTTPS))
    s3.setEndpoint(s3CoweboConfig.host)
    s3
  }

  @Provides
  def provideAmazonS3Client(s3Config: S3Config): AmazonS3Client = {
    val awsCredentials = new BasicAWSCredentials(s3Config.key, s3Config.secret)
    val config = new ClientConfiguration
    config.setSignerOverride("S3SignerType")
    val s3 = new AmazonS3Client(awsCredentials, config.withProtocol(Protocol.HTTPS))
    s3.setEndpoint(s3Config.host)
    s3
  }

  @Provides
  def provideHtml2PdfConfig(configuration: Configuration): Html2PdfConfig = {
    configuration.underlying.as[Html2PdfConfig]("html2Pdf")
  }

  @Provides
  def provideSendinBlueConfig(configuration: Configuration): SendinBlueConfig = {
    configuration.underlying.as[SendinBlueConfig]("sendinblue")
  }

  @Provides
  def provideAppConfig(configuration: Configuration): AppConfig = {
    val conf = configuration.underlying.as[AppConfig]("appconfig")
    if(conf.appUrl.endsWith("/"))
      conf.copy(appUrl = conf.appUrl.dropRight(1))
    else
      conf
  }
}
