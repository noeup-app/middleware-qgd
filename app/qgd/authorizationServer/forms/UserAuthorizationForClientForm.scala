package qgd.authorizationServer.forms

import play.api.data.Form
import play.api.data.Forms._

/**
  * The form which handles the user authorization for client to access there data with scope(s)
  */
object UserAuthorizationForClientForm {

  case class Data(
                   redirect_uri: String,
                   scope: String,
                   state: Boolean)

  val form = Form(
    mapping(
      "redirect_uri" -> nonEmptyText,
      "scope" -> nonEmptyText,
      "state" -> boolean
    )(Data.apply)(Data.unapply)
  )

}