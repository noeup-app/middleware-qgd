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

  /**
    * Convert an english word to singular
    *
    * @param word an english word
    * @return
    */
  def toSingular(word: String): String = {
    word.splitAt(word.length-2) match {
      case (name, "es") => name.last match {
        case 'i' => name.splitAt(name.length-1)._1+"y"
        case _ => name
      }
      case (_, _) => word.last match {
        case 's' => word.splitAt(word.length-1)._1
        case _ => word
      }
    }
  }
}
