package qgd.authorizationClient.controllers.signUp

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Request, Result}
import qgd.authorizationClient.controllers.results.{AjaxAuthorizationResult, AuthorizationResult, HtmlScalaViewAuthorizationResult}
import qgd.authorizationClient.forms.SignUpForm
import qgd.authorizationClient.forms.SignUpForm.signUpFormDataFormat
import qgd.authorizationClient.models.services.UserService
import qgd.resourceServer.models.Account
import qgd.utils.BodyParserHelper._
import qgd.utils.{BodyParserHelper, RequestHelper}

import scala.concurrent.Future

/**
 * The sign up controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUps @Inject()(
                                   val messagesApi: MessagesApi,
                                   val env: Environment[Account, CookieAuthenticator],
                                   userService: UserService,
                                   authInfoRepository: AuthInfoRepository,
                                   htmlSignUpsResult: HtmlSignUpsResult,
                                   ajaxSignUpsResult: AjaxSignUpsResult,
                                   avatarService: AvatarService,
                                   passwordHasher: PasswordHasher)
  extends Silhouette[Account, CookieAuthenticator] {

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def signUpAction = Action.async(jsonOrAnyContent[SignUpForm.Data]) { implicit request =>
    RequestHelper.isJson(request) match {
      case true =>
        val data: SignUpForm.Data = request.body.asInstanceOf[SignUpForm.Data]
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        signUp(loginInfo, data, ajaxSignUpsResult)
      case false =>
        SignUpForm.form.bindFromRequest.fold(
          form => Future.successful(htmlSignUpsResult.badRequest(form)),
          data => {
            val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
            signUp(loginInfo, data, htmlSignUpsResult)
          }
        )
    }
  }

  def signUp(loginInfo: LoginInfo, data: SignUpForm.Data, authorizationResult: SignUpsResult)(implicit request: Request[Any]): Future[Result] = {
    val res = userService.retrieve(loginInfo).flatMap {
      case Some(user) =>
        Future.successful(authorizationResult.userAlreadyExists())
      case None =>
        val authInfo = passwordHasher.hash(data.password)
        val user = Account(
          id = UUID.randomUUID(),
          loginInfo = Some(loginInfo),
          firstName = Some(data.firstName),
          lastName = Some(data.lastName),
          fullName = Some(data.firstName + " " + data.lastName),
          email = Some(data.email),
          scopes = List(),
          roles = List(),
          avatarURL = None,
          deleted = false
        )
        for {
          avatar <- avatarService.retrieveURL(data.email)
          user <- userService.save(user.copy(avatarURL = avatar))
          authInfo <- authInfoRepository.add(loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(loginInfo)
          value <- env.authenticatorService.init(authenticator)
          result <- env.authenticatorService.embed(value, authorizationResult.userSuccessfullyCreated())
        } yield {
          env.eventBus.publish(SignUpEvent(user, request, request2Messages))
          env.eventBus.publish(LoginEvent(user, request, request2Messages))
          result
        }
    }
    res.recover{
      case e: Exception => {
        Logger.error("An exception occurred", e)
        authorizationResult.manageError(e)
      }
    }
  }
}
