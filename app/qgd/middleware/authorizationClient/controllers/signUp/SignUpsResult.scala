package qgd.middleware.authorizationClient.controllers.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import qgd.middleware.authorizationClient.controllers.AuthorizationResult
import qgd.middleware.authorizationClient.forms.SignUpForm
import qgd.middleware.authorizationClient.forms.SignUpForm.Data
import qgd.middleware.models.Account

/**
  * Define all HTTP results (Json/Html)
  */
trait SignUpsResult extends AuthorizationResult {
  def badRequest(form: Form[SignUpForm.Data])(implicit request: Request[Any]): Result
  def userAlreadyExists(): Result
  def userSuccessfullyCreated(): Result
  def manageError(e: Exception): Result
  def userIsConnected(): Result
  def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result
}


/**
  * Define HTML results
 *
  * @param messagesApi
  * @param env
  */
class HtmlSignUpsResult @Inject() (
                                     val messagesApi: MessagesApi,
                                     val env: Environment[Account, CookieAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data])(implicit request: Request[Any]): Result =
    BadRequest(qgd.middleware.authorizationClient.views.html.signUp(form))

  override def userSuccessfullyCreated(): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.application.routes.Applications.index())

  override def manageError(e: Exception): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.signUp.routes.SignUps.signUpAction())
      .flashing("error" -> Messages("internal.server.error"))

  override def userAlreadyExists(): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.signUp.routes.SignUps.signUpAction())
      .flashing("error" -> Messages("user.exists"))

  override def userIsConnected(): Result =
    Redirect(qgd.middleware.authorizationClient.controllers.application.routes.Applications.index())

  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok(qgd.middleware.authorizationClient.views.html.signUp(SignUpForm.form))
}


/**
  * Define Json results
 *
  * @param messagesApi
  * @param env
  */
class AjaxSignUpsResult @Inject() (
                                     val messagesApi: MessagesApi,
                                     val env: Environment[Account, CookieAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data])(implicit request: Request[Any]): Result =
    BadRequest("Incorrect or incomplete login information provided")

  override def userSuccessfullyCreated(): Result =
    Ok("User successfully created") // TODO Should return User Json

  override def manageError(e: Exception): Result =
    InternalServerError(Messages("internal.server.error"))

  override def userAlreadyExists(): Result =
    BadRequest("User already exists")

  override def userIsConnected(): Result = Ok("User is connected")

  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok("User is not registered")
}