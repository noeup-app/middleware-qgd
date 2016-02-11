package qgd.authorizationClient.results

import com.google.inject.ImplementedBy
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.data.Form
import play.api.mvc.{AnyContent, Results, Request, Result}
import qgd.authorizationClient.forms.{SignUpForm, SignInForm}
import qgd.resourceServer.models.Account


/**
  * Define results (responses HTTP) from the AuthorizationClient
  */
@ImplementedBy(classOf[HtmlScalaViewAuthorizationResult])
trait AuthorizationResult extends Results with Silhouette[Account, CookieAuthenticator]{

  /**
    * Manage exceptions that can occur in sign in
    *
    *  For example : JedisConnectionException: Could not get a resource from the pool
    *
    * @param e the exception
    * @return the result with the error managed
    */
  def manageErrorSignIn(e: Exception): Result

  /**
    * Manage exceptions that can occur in sign up
    *
    *  For example : JedisConnectionException: Could not get a resource from the pool
    *
    * @param e the exception
    * @return the result with the error managed
    */
  def manageErrorSignUp(e: Exception): Result

  /**
    * BadRequest sign in when sent data are incorrect or incomplete
    */
  def badRequestSignIn(form: Form[SignInForm.Data])(implicit request: Request[Any]): Result

  /**
    * BadRequest sign up when sent data are incorrect or incomplete
    */
  def badRequestSignUp(form: Form[SignUpForm.Data])(implicit request: Request[Any]): Result

  /**
    * Called when user already exist
    */
  def userAlreadyExists(): Result

  /**
    * Called when user is successfully created
    */
  def userSuccessfullyCreated(): Result

  /**
    * Called when user is successfully logged
    */
  def userIsAuthenticated(): Result

  /**
    * Called when sent credentials are invalid
    */
  def invalidCredentials(): Result

  /**
    * Used to get protected resource
    * WARNING : probably temporary
    */
  def getResource(implicit request: SecuredRequest[AnyContent]): Result

  /**
    * Action to do if user is connected
    */
  def userIsConnected(): Result

  /**
    * Action to do if user is not connected
    */
  def userIsNotConnected(implicit request: UserAwareRequest[AnyContent]): Result

  /**
    * Action to do if user is not registered
    */
  def userIsNotRegistered(implicit request: UserAwareRequest[AnyContent]): Result

  /**
    * Action to do after signOut
    */
  def userSignOut(): Result

  /**
    * Called when user is authenticated
    */
  def userSuccessfullyAuthenticated(): Result

  /**
    * Called when an unexpected error occurred
    */
  def unexpectedProviderError(): Result
}
