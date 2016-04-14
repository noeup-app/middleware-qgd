package qgd.middleware.authorizationServer.forms

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the submission of the credentials.
 */
object SignInProviderForm {

  /**
   * The provider login form
   */
  val form = Form(
    mapping(
      "email"       -> email,
      "password"    -> nonEmptyText,
      "rememberMe"  -> boolean,
      "client_id"    -> text,
      "redirect_uri" -> text,
      "state"       -> text,
      "scope"       -> text
    )(Data.apply)(Data.unapply)
  )

  /**
    * The form data.
    *
    * @param email The email of the user.
    * @param password The password of the user.
    * @param rememberMe Indicates if the user should stay logged in on the next visit.
    *
    * Data from client that wants to use the provider
    * @param client_id the client id
    * @param redirect_uri the client redirect url
    * @param state the state, free value of the client
    * @param scope the scope wanted
    */
  case class Data(
    email: String,
    password: String,
    rememberMe: Boolean,
    client_id: String,
    redirect_uri: String,
    state: String,
    scope: String)
}
