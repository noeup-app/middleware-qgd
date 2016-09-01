package com.noeupapp.middleware.oauth2

import java.util.Date

import play.api.libs.json.Json


case class AccessToken(token_type: String, access_token: String, expires_in: Date, refresh_token: String)


object AccessToken {
  implicit val AccessTokenFormat = Json.format[AccessToken]
}