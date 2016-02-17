package qgd.authorizationServer.controllers

import play.api.mvc._

// TODO remove

/**
 * Provide security features for admin console
 */
trait SecuredAdminConsole {

  /**
   * Retrieve the connected user.
   */
  private def username(request: RequestHeader) = request.session.get("username")


  /**
   * Redirect to login if the user in not authorized.
   * capture the original URL we want to be redirected to later on successful login
    */
  private def onUnauthorized(request: RequestHeader) = {

    val args: Seq[(String, String)] = if (request.method.equals("GET")) Seq("redirect_url" -> request.uri) else Seq()

    Results.Redirect(qgd.authorizationClient.controllers.routes.ApplicationController.signInAction()).flashing(args: _*)
  }


  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }


  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

}