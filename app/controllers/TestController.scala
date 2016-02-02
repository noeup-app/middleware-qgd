package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
import controllers.routes
import models.User
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import play.api.{Configuration, Logger}
import models.services.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class TestController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 credentialsProvider: CredentialsProvider,
                                 userService: UserService,
                                 passwordHasher: PasswordHasher,
                                 authInfoRepository: AuthInfoRepository,
                                 configuration: Configuration,
                                 socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {



  def index = Action { implicit request =>
    Ok("index")
  }


  def signUp = Action.async { implicit request =>
    val mail = "test@mail.com"
    val firstName = "Test"
    val lastName = "TEST"
    val password = "pwd"
    val loginInfo = LoginInfo(CredentialsProvider.ID, mail)
    userService.retrieve(loginInfo).flatMap {
      case Some(user) =>
        Future.successful(Ok("Already added"))
      case None =>
        val authInfo = passwordHasher.hash(password)
        val user = User(
          userID = UUID.randomUUID(),
          loginInfo = loginInfo,
          firstName = Some(firstName),
          lastName = Some(lastName),
          fullName = Some(firstName + " " + lastName),
          email = Some(mail),
          List(),
          avatarURL = None
        )
        for {
          user <- userService.save(user)
          authInfo <- authInfoRepository.add(loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(loginInfo)
          value <- env.authenticatorService.init(authenticator)
          result <- env.authenticatorService.embed(value, Ok("Success"))
        } yield {
          env.eventBus.publish(SignUpEvent(user, request, request2Messages))
          env.eventBus.publish(LoginEvent(user, request, request2Messages))
          result
        }
    }
  }


  def signOut = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))
    env.authenticatorService.discard(request.authenticator, Redirect(routes.TestController.index()))
  }


  def auth = Action.async { implicit request =>
    val credentials = Credentials("test@mail.com", "pwd")
    val rememberMe = false
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      val result = Redirect(routes.TestController.secure())
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          val c = configuration.underlying
          env.authenticatorService.create(loginInfo).map {
            case authenticator if rememberMe =>
//              authenticator.copy(
//                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
//                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
//                cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
//              )
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
        Ok(e.getMessage)
    }
  }


  def secure = SecuredAction { implicit request =>
    Logger.info(request.identity.toString)
    Logger.info(request.authenticator.toString)
    Ok("secure")
  }


  def secureAdmin = SecuredAction {
    Ok("secureAdmin")
  }



}
