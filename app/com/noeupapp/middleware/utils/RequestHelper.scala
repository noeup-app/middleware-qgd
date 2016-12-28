package com.noeupapp.middleware.utils

import java.util.Locale

import play.api.mvc.RequestHeader

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


  def getFullDomain(implicit request: RequestHeader): String = {
    val secure = if(request.secure) "s" else ""
    s"http$secure://${request.host}/"
  }

}
