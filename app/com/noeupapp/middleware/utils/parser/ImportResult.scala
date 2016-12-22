package com.noeupapp.middleware.utils.parser

import com.noeupapp.middleware.errorHandle.FailError
import play.api.libs.json._
import com.noeupapp.middleware.utils.parser.Line._


/**
  * Created by damien on 08/12/2016.
  */
case class ImportResult[T](parseErrors: List[(Line, FailError)], insertErrors: List[(Line, FailError)], warnings: List[(List[String], Line, T)], successes: List[(Line, T)]){

  def toJson()(implicit format: Format[T]): JsValue = Json.obj(
    "result" -> Json.obj(
      "total" -> Json.toJson(parseErrors.length + insertErrors.length + successes.length),
      "numberOfParseErrors" -> Json.toJson(parseErrors.length),
      "numberOfInsertErrors" -> Json.toJson(insertErrors.length),
      "numberOfWarnings" -> Json.toJson(warnings.length),
      "numberOfSuccesses" -> Json.toJson(successes.length)
    ),
    "parseErrors" -> parseErrors.map(e =>
      Json.obj("line" -> Json.toJson(e._1),
        "error" -> e._2.message)
    ),
    "insertErrors" -> insertErrors.map(e =>
      Json.obj("line" -> Json.toJson(e._1),
        "error" -> e._2.message)
    ),
    "warnings" -> warnings.map(e =>
      Json.obj("errors" -> Json.toJson(e._1),
        "line" -> Json.toJson(e._2),
        "value" -> Json.toJson(e._3))
    ),
    "successes" -> successes.map(e =>
      Json.obj("line" -> Json.toJson(e._1),
        "value" -> Json.toJson(e._2))
    )
  )
}

