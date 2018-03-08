package com.noeupapp.middleware.errorHandle

import play.api.Logger
import play.api.libs.json.Json


case class ErrorResult(code: Int, message: String) {
  def toJson = Json.toJson(this)(ErrorResult.ErrorResultFormat)
}

object ErrorResult {

  private val logger = Logger("ApiError").logger

  implicit val ErrorResultFormat = Json.format[ErrorResult]

}