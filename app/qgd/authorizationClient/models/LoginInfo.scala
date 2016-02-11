package qgd.authorizationClient.models

import com.mohiva.play.silhouette.api
import anorm.SqlParser._
import anorm._

object LoginInfo {

  val parse = {
    get[String]("provider_id") ~
    get[String]("provider_key") map {
      case provider_id ~ provider_key => api.LoginInfo(provider_id, provider_key)
    }
  }

}
