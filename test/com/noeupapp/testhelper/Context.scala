package com.noeupapp.testhelper

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.noeupapp.middleware.authorizationClient.{FakeScopeAndRoleAuthorization, ScopeAndRoleAuthorization}
import com.noeupapp.middleware.entities.entity.Account
import net.codingwell.scalaguice.ScalaModule
import org.specs2.matcher.Scope
import play.api.Mode
import play.api.db.{Database, Databases}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
//import play.api.Play.current

/**
  * The context.
  */
trait Context extends Scope {

  /**
    * A fake Guice module.
    */
  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[Account, CookieAuthenticator]].toInstance(env)
      bind[ScopeAndRoleAuthorization].to[FakeScopeAndRoleAuthorization]
    }
  }

  val loginInfo = LoginInfo("qgd_test", "qgd_test")

  /**
    * An identity.
    */
  val identity = Account(
    id = UUID.randomUUID(),
    loginInfo = Some(loginInfo),
    firstName = None,
    lastName = None,
    fullName = None,
    email = None,
    scopes = List(),
    roles = List(),
    avatarURL = None,
    deleted = false
  )

  /**
    * A Silhouette fake environment.
    */
  implicit val env: Environment[Account, CookieAuthenticator] = {
    identity.loginInfo match {
      case Some(li) =>
        new FakeEnvironment[Account, CookieAuthenticator](Seq(li -> identity))
      case None => throw new RuntimeException("identity.loginInfo is None")
    }

  }


//
//  val config = Play.application.configuration
//
//  val db_driver: String = config.getString("db.test.driver").getOrElse("")
//  val db_url: String    = config.getString("db.test.url").getOrElse("")
//  val db_name: String   = config.getString("pg.db").getOrElse("")
//  val db_config: Map[String, String] = Map(©
//    "username" -> config.getString("db.test.username").getOrElse(""),
//    "password" -> config.getString("db.test.password").getOrElse("")
//  ).withDefaultValue("")

  val db_driver: String = "org.postgresql.Driver"
  val db_url: String    = "jdbc:postgresql://localhost:5432/qgd_test"
  val db_name: String   = "qgd_test"
  val db_config: Map[String, String] = Map(
    "username" -> "damien",
    "password" -> ""
  ).withDefaultValue("")


  /**
    * The application.
    */
  lazy val application = new GuiceApplicationBuilder()
    .configure(Map(
        "db.default.url"      -> db_url
      , "db.default.username" -> db_config.get("username").get
      , "db.default.password" -> db_config.get("password").get
    ))
    .in(Mode.Test)
    .overrides(new FakeModule)
    .build()


//  def withMyDatabase[T](block: Database => T) = {
//    Databases.withInMemory(
//      name = "qgd_test",
//      urlOptions = Map(
//        "MODE" -> "POSTGRESQL"
//      ),
//      config = Map(
//        "logStatements" -> true
//      )
//    )(block)
//  }

  def withDatabase[T](block: Database => T) = {

    Databases.withDatabase(
      driver = db_driver,
      url = db_url,
      name = db_name,
      config = db_config
    )(block)
  }

  implicit val context = this
}
