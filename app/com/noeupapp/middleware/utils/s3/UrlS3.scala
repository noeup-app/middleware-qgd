package com.noeupapp.middleware.utils.s3

import play.api.libs.json.Json

case class UrlS3(url: String, expirationInMinutes: Long)

object UrlS3 {
  implicit val UrlS3Format = Json.format[UrlS3]
}
