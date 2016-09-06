package com.noeupapp.middleware.errorHandle

import play.api.libs.json.Json


case class ErrorResult(code: Int, message: String) {
  def toJson = Json.toJson(this)(ErrorResult.ErrorResultFormat)
}

object ErrorResult{

  implicit val ErrorResultFormat = Json.format[ErrorResult]

}