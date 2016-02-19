package qgd.middleware.utils

import play.api.libs.json.Reads
import play.api.mvc.BodyParsers.parse


object BodyParserHelper {

  /**
    * Parse Json or any content
    *
    * @tparam T the json type you want to get
    * @return the parse according to the request content type
    */
  def jsonOrAnyContent[T : Reads] = parse.using { request =>
      RequestHelper.isJson(request) match {
        case true => play.api.mvc.BodyParsers.parse.json[T]
        case _ => play.api.mvc.BodyParsers.parse.anyContent
      }
  }
}
