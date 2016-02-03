package qgd.authorizationClient.results

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.User
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import qgd.authorizationClient.controllers.routes
import qgd.authorizationClient.forms.{SignUpForm, SignInForm}
import scala.language.implicitConversions
import scala.concurrent.Future


/**
  * Return Html Scala View as a result (response HTTP)
  */
class HtmlScalaViewAuthorizationResult @Inject() (
                                                   val messagesApi: MessagesApi,
                                                   val env: Environment[User, CookieAuthenticator],
                                                   socialProviderRegistry: SocialProviderRegistry
  ) extends AuthorizationResult {
  /**
    * Action to do if user is connected
    */
  override def userIsConnected(): Result =
    Redirect(routes.ApplicationController.index())

  /**
    * BadRequest sign in when sent data are incorrect or incomplete
    */
  override def badRequestSignIn(form: Form[SignInForm.Data])(implicit request: Request[Any]): Result =
    BadRequest(qgd.authorizationClient.views.html.signIn(form, socialProviderRegistry))

  /**
    * BadRequest sign up when sent data are incorrect or incomplete
    */
  override def badRequestSignUp(form: Form[SignUpForm.Data])(implicit request: Request[Any]): Result =
    BadRequest(qgd.authorizationClient.views.html.signUp(form))

  /**
    * Called when user already exist
    */
  override def userAlreadyExists(): Result =
    Redirect(routes.ApplicationController.signUpAction())
      .flashing("error" -> Messages("user.exists"))

  /**
    * Called when user is successfully created
    */
  override def userSuccessfullyCreated(): Result = Redirect(routes.ApplicationController.index())

  /**
    * Called when user is successfully logged
    */
  override def userIsAuthenticated(): Result = Redirect(routes.ApplicationController.index())

  /**
    * Called when sent credentials are invalid
    */
  override def invalidCredentials(): Result =
    Redirect(routes.ApplicationController.signInAction())
      .flashing("error" -> Messages("invalid.credentials"))

  /**
    * Used to get protected resource
    * WARNING : probably temporary
    */
  override def getResource(implicit request: SecuredRequest[AnyContent]): Result =
    Ok(qgd.authorizationClient.views.html.home(request.identity))


  /**
    * Action to do if user is not connected
    */
  override def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok(qgd.authorizationClient.views.html.signIn(SignInForm.form, socialProviderRegistry))


  /**
    * Action to do if user is not registered
    */
  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result =
    Ok(qgd.authorizationClient.views.html.signUp(SignUpForm.form))

  /**
    * Action to do after signOut
    */
  override def userSignOut(): Result =
    Redirect(routes.ApplicationController.index())

  /**
    * Called when user is authenticated
    */
  override def userSuccessfullyAuthenticated(): Result =
    Redirect(routes.ApplicationController.index())

  /**
    * Called when an unexpected error occurred
    */
  override def unexpectedProviderError(): Result =
    Redirect(routes.ApplicationController.signInAction()).flashing("error" -> Messages("could.not.authenticate"))

}
