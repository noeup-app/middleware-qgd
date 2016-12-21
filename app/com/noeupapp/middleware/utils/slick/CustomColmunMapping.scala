package com.noeupapp.middleware.utils.slick

import play.api.libs.json.{Json, JsValue}


import slick.driver.PostgresDriver.api._

/**
  * Created by Timbreos on 21/12/2016.
  */
object CustomColmunMapping {

  implicit val jsValueColumnType = MappedColumnType.base[JsValue, String](
    { jsValue => jsValue.toString() },
    { string => Json.parse(string) }
  )

}
