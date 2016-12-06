package com.noeupapp.middleware.utils.slick

import slick.jdbc.{GetResult, PositionedResult}


/**
  * Is able to convert each row get from a plain sql request into a Map(colName -> colValue)
  *
  * Example of usage :
  *    sql"""SELECT ...."""
  *      .as(ResultMap)
  *
  */
object ResultMap extends GetResult[Map[String,Any]] {
  def apply(pr: PositionedResult): Map[String, AnyRef] = {
    val rs = pr.rs // <- jdbc result set
    val md = rs.getMetaData
    (1 to pr.numColumns).map{ i=> md.getColumnName(i) -> rs.getObject(i) }.toMap
  }
}