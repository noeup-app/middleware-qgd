package qgd.authorizationServer.models


/**
  * Defines a list of authorization Grant Type as described in RCF
  *
  * Authorization Code for apps running on a web server (prefered)
  * Implicit for browser-based or mobile apps
  * Password for logging in with a username and password
  * Client credentials for application access
  */
object GrantType extends Enumeration {
  type GrantType = Value
  val authorization_code, `implicit`, resource_owner_password, credentials, client_credentials = Value
}
//  trait GrantType {
//    case object authorization_code      extends GrantType
//    case object `implicit`              extends GrantType
//    case object resource_owner_password extends GrantType
//    case object credentials             extends GrantType
//    case object client_credentials      extends GrantType
//  }



