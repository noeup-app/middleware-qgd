package qgd.authorizationServer.forms

import play.api.data.Form
import play.api.data.Forms._

  /**
    * The form which handles the
    */
  object AppAuthInfo {

    /**
      *
      * @param redirect_uri
      * @param scope
      * @param state
      */
    case class Data(
                     redirect_uri: String,
                     scope: String,
                     state: Boolean)


    /**
      * A play framework form.
      */
    val form = Form(
      mapping(
              "redirect_uri" -> nonEmptyText,
              "scope" -> nonEmptyText,
              "state" -> boolean
            )(Data.apply)(Data.unapply)
    )

  }