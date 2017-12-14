package com.noeupapp.middleware.entities.account

import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.entities.organisation.Organisation
import com.noeupapp.middleware.entities.user.User
import play.api.libs.json.Json
import com.mohiva.play.silhouette.api.LoginInfo._
import com.noeupapp.middleware.entities.organisation.Organisation._


case class Account(
                    loginInfo: api.LoginInfo,
                    user: User,
                    roles: List[String],
                    organisation: Option[Organisation]) extends api.Identity


object Account {
  implicit val AccountFormat = Json.format[Account]
}

