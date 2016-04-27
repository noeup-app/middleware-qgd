package com.noeupapp.middleware.entities.account

import java.util.UUID

import anorm.SqlParser._
import anorm._
import com.mohiva.play.silhouette.api
import com.noeupapp.middleware.entities.organisation.Organisation
import com.noeupapp.middleware.entities.user.User
import play.api.libs.json.Json
import com.noeupapp.middleware.authorizationClient.login.LoginInfo._
import com.mohiva.play.silhouette.api.LoginInfo._
import com.noeupapp.middleware.entities.organisation.Organisation._

import scala.language.postfixOps

case class Account(
                 loginInfo: api.LoginInfo,
                 user: User,
                 organisation: Option[Organisation]) extends api.Identity


object Account {
  implicit val AccountFormat = Json.format[Account]
}

