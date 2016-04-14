package qgd.middleware.authorizationServer.forms

import play.api.data.Form
import play.api.data.Forms._
import qgd.middleware.authorizationServer.models.RequestAuthInfo

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
