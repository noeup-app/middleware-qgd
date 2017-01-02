package com.noeupapp.middleware.crudauto

import java.util.UUID

import anorm.SQL
import com.google.inject.{AbstractModule, Inject}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import com.noeupapp.middleware.authorizationClient.{FakeScopeAndRoleAuthorization, ScopeAndRoleAuthorization}
import com.noeupapp.middleware.crudauto.model._
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.relationUserRole.RelationUserRoleDAO
import com.noeupapp.middleware.entities.role.{RoleDAO, RoleService}
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError.Expect
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
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._

import scala.concurrent.Future
import scalaz.\/-

/**
  * The context.
  */

object CrudAutoContext {
  type RoleList = List[String]
}

import CrudAutoContext._


trait CustomizableCrudAutoContext extends Scope {


  def provideCrudClassName: CrudClassName
  def provideRoleList: RoleList = List.empty


  /**
    * A fake Guice module.
    */
  class GlobalBindingsModule(provideRoleList: () => RoleList) extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[Account, BearerTokenAuthenticator]].toInstance(env)
      bind[ScopeAndRoleAuthorization].to[FakeScopeAndRoleAuthorization]
      bind[CrudClassName].toInstance(provideCrudClassName)
      bind[RoleList].toInstance(provideRoleList())
      bind[RoleService].to[MockRoleService]
    }
  }





  val loginInfo = LoginInfo("np_test", "np_test")

  /**
    * An identity.
    */
  val id = Account(
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
    new FakeEnvironment[Account, BearerTokenAuthenticator](Seq(loginInfo -> id))
  }

  val db_driver: String = "org.postgresql.Driver"
  val db_url: String    = "jdbc:postgresql://localhost:5432/test"
  val db_name: String   = "test"
  val db_config: Map[String, String] = Map(
    "username" -> "guillaume",
    "password" -> ""
  ).withDefaultValue("")


  val db_host = "localhost"
  val db_port = 5432


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
      , "slick.dbs.default.db.user"     -> "guillaume"
      , "slick.dbs.default.db.password" -> ""
    ))
    .in(Mode.Test)
    .overrides(new GlobalBindingsModule(() => provideRoleList))
    .build()

  val injector = application.injector


  val dao = injector.instanceOf[Dao]


  val pk = UUID.randomUUID()

  def createTables = {
    for {
      _ <- Thing.createTable(dao.db)
      _ <- Test.createTable(dao.db)
      _ <- RelTestThing.createTable(dao.db)
      _ <- AuthTest.createTable(dao.db)
    } yield ()
  }
  def populate = Test.populate(dao.db, pk, identity[Seq[Test]], identity[Seq[Thing]])
  def populate(f: (Seq[Test]) => Seq[Test]) = Test.populate(dao.db, pk, f, identity[Seq[Thing]])
  def populate(f: (Seq[Test]) => Seq[Test], g: (Seq[Thing]) => Seq[Thing]) =
    Test.populate(dao.db, pk, f, g)
  def allTests = Test.all(dao.db, pk)
  def allThings = Thing.all(dao.db, pk)
  def allRel = RelTestThing.all(dao.db, pk)
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

  implicit val context: Scope = this
}

class MockRoleService @Inject()(roleList: RoleList,
                                relationUserRole: RelationUserRoleDAO,
                                roleDAO: RoleDAO) extends RoleService(relationUserRole, roleDAO) {
  override def getRoleByUser(userId: UUID): Future[Expect[List[String]]] =
    Future.successful(\/-(roleList))
}

trait CrudAutoContext extends CustomizableCrudAutoContext {

  override def provideCrudClassName: CrudClassName = new Crud

}




class Crud extends CrudClassName {

  override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
    "tests"  -> configuration[Test, UUID, TestTableDef]
        .withOpenAccess,
    "things" -> configuration[Thing, UUID, ThingTableDef]
      .withOpenAccess,
    "rel"    -> configuration[RelTestThing, Long, RelTestThingTableDef]
      .withOpenAccess,
    "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
      .withOpenAccess
  )

}
