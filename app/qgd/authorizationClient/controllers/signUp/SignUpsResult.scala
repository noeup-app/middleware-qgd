package qgd.authorizationClient.controllers.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Result
import qgd.authorizationClient.controllers.results.AuthorizationResult
import qgd.authorizationClient.forms.SignUpForm
import qgd.authorizationClient.forms.SignUpForm.Data
import qgd.resourceServer.models.Account

/**
  * Define all HTTP results (Json/Html)
  */
trait SignUpsResult extends AuthorizationResult {
  def badRequest(form: Form[SignUpForm.Data]): Result
  def userAlreadyExists(): Result
  def userSuccessfullyCreated(): Result
  def manageError(e: Exception): Result
}


/**
  * Define HTML results
  * @param messagesApi
  * @param env
  */
class HtmlSignUpsResult @Inject() (
                                     val messagesApi: MessagesApi,
                                     val env: Environment[Account, CookieAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data]): Result =
    BadRequest(qgd.authorizationClient.views.html.signUp(form))

  override def userSuccessfullyCreated(): Result =
    Redirect(routes.ApplicationController.index())

  override def manageError(e: Exception): Result =
    Redirect(routes.ApplicationController.signUpAction())
      .flashing("error" -> Messages("internal.server.error"))

  override def userAlreadyExists(): Result =
    Redirect(routes.ApplicationController.signUpAction())
      .flashing("error" -> Messages("user.exists"))
}


/**
  * Define Json results
  * @param messagesApi
  * @param env
  */
class AjaxSignUpsResult @Inject() (
                                     val messagesApi: MessagesApi,
                                     val env: Environment[Account, CookieAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data]): Result =
    BadRequest("Incorrect or incomplete sign in information provided")

  override def userSuccessfullyCreated(): Result =
    Ok("User successfully created")

  override def manageError(e: Exception): Result =
    InternalServerError(Messages("internal.server.error"))

  override def userAlreadyExists(): Result =
    BadRequest("User already exists")
}