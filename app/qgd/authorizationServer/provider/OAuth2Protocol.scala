package qgd.authorizationServer

package object provider {

  /**
   * OAuth2 Specification for Response types used in the /authorize endpoint.
   * See http://tools.ietf.org/html/rfc6749 for the full spec
   */
  object ResponseTypes {
    /**
     * "code" is used in the authorization grant flow for requesting an authorization_code
     */
    val CODE = "code"

    /**
     * "token" is used in the implicit grant flow to requesting an access_token
     */
    val TOKEN = "token"
  }

  object AuthorizeParameters {

    /**
     * Required by the OAuth2Spec
     */
    val CLIENT_ID = "client_id"
    /**
     * Required by the OAuth2Spec
     */
    val RESPONSE_TYPE = "response_type"

    /**
     * Optional by the OAuth2Spec, required by qgd
     */
    val STATE = "state"

    /**
     * Optional
     */
    val REDIRECT_URI = "redirect_uri"

    /**
     * Optional
     */
    val SCOPE = "scope"

  }

}