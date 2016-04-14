package com.noeupapp.middleware.authorizationClient.login

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api

object LoginInfo {

  val parse = {
    get[String]("provider_id") ~
    get[String]("provider_key") map {
      case provider_id ~ provider_key => api.LoginInfo(provider_id, provider_key)
    }
  }

  // TODO rapatriate DAO !!!!

}
