package qgd.middleware.utils

import java.net.URLEncoder

object UrlHelper {

  /**
    * Encode a string to use it as a query parameter un URL. Encoding in UTF-8
    * <em><strong>Note:</strong> The <a href=
    * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
    * World Wide Web Consortium Recommendation</a> states that
    * UTF-8 should be used. Not doing so may introduce
    * incompatibilities.</em>
    *
    * @param stringToEncode the string to encode
    * @return encoded string
    */
  def encodeUrlUtf8(stringToEncode: String): String =
    URLEncoder.encode(stringToEncode, "UTF-8")


}
