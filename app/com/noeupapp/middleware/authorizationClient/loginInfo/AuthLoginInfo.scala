package com.noeupapp.middleware.authorizationClient.loginInfo

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import play.api.libs.json.Json



case class AuthLoginInfo(providerId: String,
                         providerKey: String,
                         user: UUID)



object AuthLoginInfo {

  implicit val authLoginInfoFormat = Json.format[AuthLoginInfo]

  val parse = {
    get[String]("provider_id") ~
      get[String]("provider_key") ~
      get[UUID]("user") map {
      case provider_id ~ provider_key ~ user => AuthLoginInfo(provider_id, provider_key, user)
    }
  }

  def fromLoginInfo(loginInfo: api.LoginInfo, user: UUID) =
    AuthLoginInfo(loginInfo.providerID, loginInfo.providerKey, user)

}

