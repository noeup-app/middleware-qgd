package com.noeupapp.middleware.crudauto

import java.util.UUID

import anorm.SQL
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.noeupapp.middleware.authorizationClient.{FakeScopeAndRoleAuthorization, ScopeAndRoleAuthorization}
import com.noeupapp.middleware.crudauto.model.{Test, TestTableDef, Thing, ThingTableDef}
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.User
import net.codingwell.scalaguice.ScalaModule
import org.joda.time.DateTime
import org.specs2.matcher.Scope
import play.api.{Logger, Mode}
import play.api.db.DB
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import play.api.Play.current
import slick.driver._
import slick.driver.PostgresDriver.api._

/**
  * The context.
  */
trait CrudAutoContext extends Scope {

  /**
    * A fake Guice module.
    */
  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[Account, BearerTokenAuthenticator]].toInstance(env)
      bind[ScopeAndRoleAuthorization].to[FakeScopeAndRoleAuthorization]
      bind[CrudClassName].toInstance(new Crud)
    }
  }



  val loginInfo = LoginInfo("qgd_test", "qgd_test")

  /**
    * An identity.
    */
  val identity = Account(
    loginInfo = loginInfo,
    user = User(
      id = UUID.randomUUID(),
      firstName = None,
      lastName = None,
      email = None,
      avatarUrl = None,
      created = new DateTime(),
      active = false,
      deleted = false
    ),
    None
  )

  /**
    * A Silhouette fake environment.
    */
  implicit val env: Environment[Account, BearerTokenAuthenticator] = {
    new FakeEnvironment[Account, BearerTokenAuthenticator](Seq(loginInfo -> identity))
  }

  val db_driver: String = "org.postgresql.Driver"
  val db_url: String    = "jdbc:postgresql://localhost:5432/qgd_test"
  val db_name: String   = "qgd_test"
  val db_config: Map[String, String] = Map(
    "username" -> "damien",
    "password" -> ""
  ).withDefaultValue("")


  val db_host = "localhost"
  val db_port = 6379


  /**
    * The application.
    */
  lazy val application = new GuiceApplicationBuilder()
    .configure(Map(
        "play.http.router"              -> "crud.Routes"
//      , "db.default.url"              -> db_url
//      , "db.default.username"         -> db_config("username")
//      , "db.default.password"         -> db_config("password")
      , "slick.dbs.default.driver"      -> "slick.driver.PostgresDriver$"
      , "slick.dbs.default.db.driver"   -> "org.postgresql.Driver"
      , "slick.dbs.default.db.url"      -> db_url//s"jdbc:postgresql://$db_host:$db_port/$db_name"
      , "slick.dbs.default.db.user"     -> "damien"
      , "slick.dbs.default.db.password" -> ""
    ))
    .in(Mode.Test)
    .overrides(new FakeModule)
    .build()

  val injector = application.injector


  implicit val context = this




  val dao = injector.instanceOf[Dao]


  val pk = UUID.randomUUID()

  def createTables = {
    for {
      _ <- Thing.createTable(dao.db)
      _ <- Test.createTable(dao.db)
    } yield ()
  }
  def populate = Test.populate(dao.db, pk)
  def allTests = Test.all(dao.db, pk)
  def allThings = Thing.all(dao.db, pk)
  def dropTable = Test.dropTable(dao.db)




  def sameElementsAs[A](s1: Seq[A], s2: Seq[A]) = {
    val res = s1.forall(s2.contains) && s2.forall(s1.contains)
    if(! res){
      Logger.error("")
      Logger.error(s"Expected : $s1")
      Logger.error("")
      Logger.error(s"Found : $s2")
      Logger.error("")
    }
    res
  }


}




class Crud extends CrudClassName {

  override def configure(modelName: String): Option[CrudConfiguration[_,_,_]] =
    modelName match {
      case "tests" => configuration[Test, UUID, TestTableDef]
      case "things" => configuration[Thing, UUID, ThingTableDef]
      case _ => None
    }
}
