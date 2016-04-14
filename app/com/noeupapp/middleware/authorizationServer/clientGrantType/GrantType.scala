package com.noeupapp.middleware.authorizationServer.clientGrantType

import anorm.ToStatement
import play.api.libs.json.Format
import com.noeupapp.middleware.utils.EnumUtils

import scala.collection.breakOut


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

  val authorization_code, `implicit`, resource_owner_password, credentials, client_credentials, default = Value

  implicit val grantTypeFormat: Format[GrantType] = EnumUtils.enumFormat(GrantType)
  implicit val grantTypeToSQL: ToStatement[GrantType] = EnumUtils.toStatement(_.toString)

  def fromString(str: String): GrantType = {
    val enumList: List[GrantType]  = this.values.toList
    val enumStringList: List[String] = enumList.map(_.toString)

    val enumValueStringLink: Map[String, GrantType] = (enumStringList zip enumList)(breakOut)
    enumValueStringLink.getOrElse(str, default)
    // TODO need to add default grant type, which is not in RFC ! Need o find something else
  }

}