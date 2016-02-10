package qgd.authorizationClient.utils

import java.util.Locale

import play.api.mvc.{RequestHeader, Request}

object RequestHelper {

  /**
    * This check if a request is a json one
 *
    * @param request the request to test
    * @return true if the request is json ; false otherwise
    */
  def isJson(request: RequestHeader): Boolean = {
    request.contentType.map(_.toLowerCase(Locale.ENGLISH)) match {
      case Some("application/json") | Some("text/json") =>
        true
      case _ =>
        false
    }
  }

}
