package qgd.authorizationClient.results

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, AnyContent, Result}
import qgd.authorizationClient.forms.SignInForm
import qgd.authorizationClient.forms.SignUpForm.Data


/**
  * Return Ajax as a result (response HTTP)
  */
class AjaxAuthorizationResult  @Inject() (
                                           val messagesApi: MessagesApi,
                                           val env: Environment[User, CookieAuthenticator])
  extends AuthorizationResult {

  /**
    * Action to do if user is connected
    */
  override def userIsConnected(): Result = ???

  /**
    * Action to do if user is not registered
    */
  override def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result = ???

  /**
    * Action to do after signOut
    */
  override def userSignOut(): Result = ???

  /**
    * Called when user is successfully logged
    */
  override def userIsAuthenticated(): Result = ???

  /**
    * Called when user is successfully created
    */
  override def userSuccessfullyCreated(): Result = ???

  /**
    * Called when sent credentials are invalid
    */
  override def invalidCredentials(): Result = ???

  /**
    * Action to do if user is not connected
    */
  override def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result = ???

  /**
    * Called when user already exist
    */
  override def userAlreadyExists(): Result = ???

  /**
    * Used to get protected resource
    * WARNING : probably temporary
    */
  override def getResource(implicit request: SecuredRequest[AnyContent]): Result = ???

  /**
    * BadRequest sign in when sent data are incorrect or incomplete
    */
  override def badRequestSignIn(form: Form[SignInForm.Data])(implicit request: Request[AnyContent]): Result = ???

  /**
    * BadRequest sign up when sent data are incorrect or incomplete
    */
  override def badRequestSignUp(form: Form[Data])(implicit request: Request[AnyContent]): Result = ???
}
