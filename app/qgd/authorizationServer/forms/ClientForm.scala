package qgd.authorizationServer.forms

import play.api.data.Form
import play.api.data.Forms._
import qgd.authorizationServer.models.Client


/**
  * Form used to manage clients (add, update)
  */
object ClientForm {

  val form = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "secret" -> nonEmptyText,
      "grantType" -> optional(text),
      "description" -> nonEmptyText,
      "redirectUri" -> nonEmptyText,
      "scope" -> optional(text)
    )(Client.apply)(Client.unapply))
}
