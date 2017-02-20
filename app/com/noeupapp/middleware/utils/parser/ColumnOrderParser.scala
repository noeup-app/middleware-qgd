package com.noeupapp.middleware.utils.parser

class ColumnOrderParser {

  /**
    * Parse an string that represents column order
    * @param str serialised list with maybe empty values
    *            ex : "1,2,,5,6"
    * @return the list unserialized
    *         ex : List(Some(1), Some(2), None, Some(5), Some(6))
    */
  def parse(str: Option[String]): List[Option[Int]] =
    str.map(_.split(',').toList.map{
      case "" => None
      case int => Some(int.toInt)
    }).getOrElse(List.empty)

}
