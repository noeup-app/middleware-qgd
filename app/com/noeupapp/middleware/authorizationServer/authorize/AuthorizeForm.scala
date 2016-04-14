package com.noeupapp.middleware.authorizationServer.authorize

import play.api.data.Form
import play.api.data.Forms._

object AuthorizeForm {

  val form = Form(
    mapping(
            "client_id" -> nonEmptyText,
            "redirect_uri" -> nonEmptyText,
            "scope" -> text,
            "state" -> text,
            "accepted" -> text
    )(RequestAuthInfo.apply)(RequestAuthInfo.unapply))
}
