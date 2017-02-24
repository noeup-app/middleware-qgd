package com.noeupapp.middleware.authorizationClient.loginInfo

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import play.api.libs.json.Json



object LoginInfo {

  val parse = {
    get[String]("provider_id") ~
    get[String]("provider_key") map {
      case provider_id ~ provider_key => api.LoginInfo(provider_id, provider_key)
    }
  }
}
