package com.noeupapp.middleware.authorizationServer.authorize

import javax.inject.Inject

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.{Environment, LoginEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
import com.noeupapp.middleware.authorizationClient.login.Login
import com.noeupapp.middleware.authorizationServer.authCode.{AuthCode, AuthCodeService}
import com.noeupapp.middleware.authorizationServer.client.Client
import com.noeupapp.middleware.entities.user.{Account, AccountService, User}
import play.api.db.DB
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Action
import play.api.{Configuration, Logger}

import scala.concurrent.Future

//import models.oauth2.Client
//import play.api.db.slick.DBAction
//import play.api.data._
//import play.api.data.Forms._
import play.api.Play.current
//import oauth2.OAuthDataHandler
//import com.noeupapp.middleware.authorizationServer.models

import scala.concurrent.ExecutionContext.Implicits.global


class Authorizations @Inject()(val messagesApi: MessagesApi,
                               val env: Environment[Account, CookieAuthenticator],
                               userService: AccountService,
                               configuration: Configuration,
                               credentialsProvider: CredentialsProvider,
                               authCodeService: AuthCodeService)
    extends Silhouette[Account, CookieAuthenticator] {


  val log = play.Logger.of("application")

  val errorCodes = Map(
    "access_denied" -> "Access was denied",
    "invalid_request" -> "Request made was not valid",
    "unauthorized_client" -> "Client is not authorized to perform this action",
    "unsupported_response_type" -> "Response type requested is not allowed",
    "invalid_scope" -> "Requested scope is not allowed",
    "server_error" -> "Server encountered an error",
    "temporarily_unavailable" -> "Service is temporary unavailable")

  // TODO comments

  def authorize(client_id: String, redirect_uri: String, state: String, scope: String) = UserAwareAction { implicit request =>

    Logger.info("Authorize.authorize clientId : " + (client_id, redirect_uri, state, scope).toString())

    // check if such a client exists
    Client.findByClientId(client_id) match {

      case None => // doesn't exist
        BadRequest("No such client exists.")

      case Some(client) =>
        Logger.info("Authorize.authorize")
        // read URL parameters
        val params = List("client_id", "redirect_uri", "state", "scope")
        val data = params.map(k =>
          k -> request.queryString.getOrElse(k, Seq("")).head).toMap

        Logger.debug("Authorize.authorize, data = " + data)



        //val clientId = data("client_id")

        // User is connected ?
        request.identity match {
          case Some(user) => // User is connected, let's show him the authorize view
            val aaInfoForm = AuthorizeForm.form.bind(data)
            Logger.info(s"Auth : $aaInfoForm")
            Ok(com.noeupapp.middleware.authorizationServer.authorize.html.authorize(user.user, aaInfoForm))
          case None => // User is not connected, let's redirect him to login page
            Redirect(com.noeupapp.middleware.authorizationServer.authorize.routes.Authorizations.login(client_id, redirect_uri, state, scope))
        }
    }
  }

  // TODO comments
  // TODO secured action
  def send_auth = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) =>
        val boundForm = AuthorizeForm.form.bindFromRequest
        boundForm.fold(
          formWithErrors => {
            log.debug("Authorize.send_auth : form ko -> " + formWithErrors.errors.toString)
            Future.successful(Ok(com.noeupapp.middleware.authorizationServer.authorize.html.authorize(user.user, formWithErrors)))
          },
          aaInfo => {
            Logger.debug("Authorize.send_auth form ok")
            aaInfo.accepted match {
              case "Y" =>
                val expiresIn = Int.MaxValue
                authCodeService.generateAuthCodeForClient(
                      aaInfo.clientId, aaInfo.redirectUri, aaInfo.scope,
                      user.user.id, expiresIn) map {
                  case Some(ac) =>
                    val authCode = ac.authorizationCode
                    val state = aaInfo.state
                    Redirect(s"${aaInfo.redirectUri}?code=$authCode&state=$state")
                  case None =>
                    val errorCode = "server_error"
                    Redirect(s"${aaInfo.redirectUri}?error=$errorCode")
                }

              case "N" =>
                val errorCode = "access_denied"
                Future.successful(Redirect(s"${aaInfo.redirectUri}?error=$errorCode"))
              case _ =>
                val errorCode = "invalid_request"
                Future.successful(Redirect(s"${aaInfo.redirectUri}?error=$errorCode"))
            }
          })
      case None       =>
        Logger.info("Authorize.send_auth : Not connected")
        Future.successful(Redirect(com.noeupapp.middleware.authorizationServer.authorize.routes.Authorizations.authenticate())) // TODO add flash message
    }
  }

  def login(client_id: String, redirect_uri: String, state: String, scope: String) = Action { implicit request =>
    // Bind data to form for hidden inputs
    val form = SignInProviderForm.form.bind(Map(
      "client_id"    -> client_id,
      "redirect_uri" -> redirect_uri,
      "state"        -> state,
      "scope"        -> scope
    ))
    Ok(com.noeupapp.middleware.authorizationServer.authorize.html.signIn(form, SocialProviderRegistry(Seq())))
  }

  def authenticate() = Action.async { implicit request =>
    SignInProviderForm.form.bindFromRequest.fold(
      form => {
        Logger.warn("Authorize.authenticate form ko : " + form.errors)
        Future.successful(BadRequest(com.noeupapp.middleware.authorizationServer.authorize.html.signIn(form, SocialProviderRegistry(Seq()))))
      },
      data => {
        val authenticate = Login(data.email, data.password, data.rememberMe)
        val credentials = authenticate.getCredentials
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          Logger.info("Form data : " + data)
          val result =
            Redirect(com.noeupapp.middleware.authorizationServer.authorize.routes.Authorizations.authorize(data.client_id.toString, data.redirect_uri, data.state, data.scope))
          userService.retrieve(loginInfo).flatMap {
            case Some(user) =>
              val c = configuration.underlying
              env.authenticatorService.create(loginInfo).map {
                case authenticator if authenticate.rememberMe =>
//                  authenticator.copy(
//                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
//                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
//                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
//                  )
                  authenticator
                case authenticator => authenticator
              }.flatMap { authenticator =>
                env.eventBus.publish(LoginEvent(user, request, request2Messages))
                env.authenticatorService.init(authenticator).flatMap { v =>
                  env.authenticatorService.embed(v, result)
                }
              }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case e: ProviderException =>
            Logger.warn("Logins.authenticate failed : " + authenticate + " -> " + e.getMessage)
            Redirect(com.noeupapp.middleware.authorizationServer.authorize.routes.Authorizations.login(data.client_id.toString, data.redirect_uri, data.state, data.scope))
              .flashing("error" -> Messages("invalid.credentials"))
          case e: Exception => {
            Logger.error("An exception ocurred", e)
            Redirect(com.noeupapp.middleware.authorizationServer.authorize.routes.Authorizations.login(data.client_id.toString, data.redirect_uri, data.state, data.scope))
              .flashing("error" -> Messages("internal.server.error"))
          }
        }
      }
    )
  }
}
