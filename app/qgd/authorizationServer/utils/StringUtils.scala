package qgd.authorizationServer.utils

object StringUtils { // TODO move upstairs


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

}
