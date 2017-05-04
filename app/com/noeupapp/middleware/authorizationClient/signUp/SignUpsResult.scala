package com.noeupapp.middleware.authorizationClient.signUp

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.AuthorizationResult
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import SignUpForm.Data
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.User

/**
  * Define all HTTP results (Json/Html)
  */
trait SignUpsResult extends AuthorizationResult {
  def badRequest(form: Form[SignUpForm.Data])(implicit request: Request[Any]): Result
  def userAlreadyExists(user: User): Result
  def userSuccessfullyCreated(): Result
  def manageError(): Result
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
                                     val env: Environment[Account, BearerTokenAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data])(implicit request: Request[Any]): Result =
    BadRequest(com.noeupapp.middleware.authorizationClient.signUp.html.signUp(form))

  override def userSuccessfullyCreated(): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.signUpActionGet())
      .flashing("info" -> Messages("user.successfully.created"))

  override def manageError(): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.subscribe())
      .flashing("error" -> Messages("internal.server.error"))

  override def userAlreadyExists(user: User): Result =
    Redirect(com.noeupapp.middleware.authorizationClient.signUp.routes.SignUps.subscribe())
      .flashing("error" -> Messages("user.exists"))

  override def userIsConnected(): Result =
    Redirect(com.noeupapp.middleware.application.routes.Applications.index())

  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok(com.noeupapp.middleware.authorizationClient.signUp.html.signUp(SignUpForm.form))
}


/**
  * Define Json results
 *
  * @param messagesApi
  * @param env
  */
class AjaxSignUpsResult @Inject() (
                                     val messagesApi: MessagesApi,
                                     val env: Environment[Account, BearerTokenAuthenticator])
  extends SignUpsResult {

  override def badRequest(form: Form[Data])(implicit request: Request[Any]): Result =
    BadRequest("Incorrect or incomplete login information provided")

  override def userSuccessfullyCreated(): Result =
    Ok("User successfully created") // TODO Should return User Json

  override def manageError(): Result =
    InternalServerError(Messages("internal.server.error"))

  override def userAlreadyExists(user: User): Result =
    BadRequest("User already exists")
      .withHeaders("Location" -> com.noeupapp.middleware.entities.user.routes.Users.fetchById(user.id).toString)

  override def userIsConnected(): Result = Ok("User is connected")

  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok("User is not registered")
}