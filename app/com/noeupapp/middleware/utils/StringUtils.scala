package com.noeupapp.middleware.utils

object StringUtils {


  /**
   * Extract the subdomain as .domain.com from a given request.host property
   * @param host Request host param
   * @return string with the subdomain
   */
  def subdomain(host: String): String = {

    var domain: String = host
    if ("\\.".r.findAllMatchIn(domain).length >= 2) {

      if (host.contains(":")) {
        domain = host.substring(0, host.indexOf(":"))
      }

      val pieces = domain.split("\\.")
      if (pieces.length >= 3) {
        domain = "." + List(pieces(pieces.length - 2), pieces(pieces.length - 1)).mkString(".")
      }
    }

    domain
  }


  def snakeToCamel(s: String): String = {
    val split = s.split("_")
    val tail = split.tail.map { x => x.head.toUpper + x.tail }
    split.head + tail.mkString
  }


  def splitAndKeepEmpty(str: String, delimiter: Char): List[String] =
    str.span(_ != delimiter) match {
      case (before, "") => List(before)
      case (before, after) => before :: splitAndKeepEmpty(after.drop(1), delimiter)
    }

}
