package qgd.authorizationClient.controllers

import java.io.File
import java.util.UUID
import javax.inject.Inject

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Silhouette, Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.mohiva.play.silhouette.test._
import org.joda.time.DateTime
import play.api
import play.api.{Mode, Configuration}
import play.api.i18n.{I18nComponents, Lang, Messages, MessagesApi}
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import qgd.authorizationClient.results.{AjaxAuthorizationResult, HtmlScalaViewAuthorizationResult}
import qgd.authorizationClient.utils.{WithScopes, WithScope}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import qgd.resourceServer.models.Account


class ScopeSpec extends PlaySpecification with Mockito {
  sequential

  /**
    * The contexts.
    */
  trait Context extends Scope {

    /**
      * An identity.
      */
    val identity = Account(
      id = UUID.randomUUID(),
      loginInfo = LoginInfo("facebook", "user@facebook.com"),
      firstName = None,
      lastName = None,
      fullName = None,
      email = None,
      scopes = List(),
      roles = List(),
      avatarURL = None
    )


    def getInformations(scopes: List[String]): (Account, CookieAuthenticator) = {
      val u = identity.copy(scopes = scopes)
      val cookieAuthenticator = new CookieAuthenticator("id", identity.loginInfo, new DateTime(), new DateTime(), None, None, None)
      (u, cookieAuthenticator)
    }

    def checkAND(ok: Boolean, required_scopes: List[String], user_scopes: List[String]): Unit ={
      val res = WithScopes.isAuthorized(identity.copy(scopes = user_scopes),required_scopes: _*)
      res must beEqualTo(ok)
    }
    def checkOR(ok: Boolean, required_scopes: List[String], user_scopes: List[String]): Unit ={
      val res = WithScope.isAuthorized(identity.copy(scopes = user_scopes),required_scopes: _*)
      res must beEqualTo(ok)
    }
  }



  "Scope filter" should {
    "allow when no scope is required and when user has no scope" in  new Context {
      checkAND(ok = true, required_scopes = List(), user_scopes = List())
      checkOR(ok = true, required_scopes = List(), user_scopes = List())
    }


    "block when one scope is required and when user has no scope" in  new Context {

      checkAND(ok = false, required_scopes = List("admin"), user_scopes = List())
      checkOR(ok = false, required_scopes = List("admin"), user_scopes = List())

    }

    "block when several scopes are required and when user has no scope" in  new Context {

      checkAND(ok = false, required_scopes = List("admin", "test.mal.azdazd"), user_scopes = List())
      checkOR(ok = false, required_scopes = List("admin", "test.mal.azdazd"), user_scopes = List())

    }


    "allow when no scope is required and when user has one scope" in  new Context {

      checkAND(ok = true, required_scopes = List(), user_scopes = List("admin"))
      checkOR(ok = true, required_scopes = List(), user_scopes = List("admin"))

    }

    "allow when no scope is required and when user has several scope" in  new Context {

      checkAND(ok = true, required_scopes = List(), user_scopes = List("admin", "test.mal.azdazd"))
      checkOR(ok = true, required_scopes = List(), user_scopes = List("admin", "test.mal.azdazd"))

    }


    "allow when one scope is required and when user has the right one" in  new Context {

      checkAND(ok = true, required_scopes = List("admin"), user_scopes = List("admin"))
      checkOR(ok = true, required_scopes = List("admin"), user_scopes = List("admin"))

    }

    "block when one scope is required and when user has one but different scope" in  new Context {

      checkAND(ok = false, required_scopes = List("admin"), user_scopes = List("hello"))
      checkOR(ok = false, required_scopes = List("admin"), user_scopes = List("hello"))

    }


    "allow when one scope is required and when user has several scopes including the right one" in  new Context {

      checkAND(ok = true, required_scopes = List("hello.world.test"), user_scopes = List("hello.world", "admin"))
      checkOR(ok = true, required_scopes = List("hello.world.test"), user_scopes = List("hello.world", "admin"))

    }

    "block when one scope is required and when user has several scopes without the right one" in  new Context {

      checkAND(ok = false, required_scopes = List("hello.world.test"), user_scopes = List("world", "admin"))
      checkOR(ok = false, required_scopes = List("hello.world.test"), user_scopes = List("world", "admin"))

    }



    "[AND] block when several scopes are required and one scope is provided (user) but the provided one doesn't share any part of the required one" in  new Context {

      checkAND(ok = false, required_scopes = List("hello.world.test", "world"), user_scopes = List("admin.world"))

    }

    "[AND] block when several scopes are required and one scope is provided (user) but the provided one shares a part of the required one" in  new Context {

      checkAND(ok = false, required_scopes = List("hello.world.test", "world"), user_scopes = List("hello.world"))

    }

    "[AND] allow when several scopes are required and one scope is provided (user) but the provided one shares all of the required one" in  new Context {

      checkAND(ok = true, required_scopes = List("hello.world.test", "hello.world"), user_scopes = List("hello"))

    }


    "[OR] block when several scopes are required and one scope is provided (user) but the provided one doesn't share any part of the required one" in  new Context {

      checkOR(ok = false, required_scopes = List("hello.world.test", "world"), user_scopes = List("admin.world"))

    }

    "[OR] allow when several scopes are required and one scope is provided (user) but the provided one shares a part of the required one" in  new Context {

      checkOR(ok = true, required_scopes = List("hello.world.test", "hello"), user_scopes = List("hello.world"))

    }

    "[OR] allow when several scopes are required and one scope is provided (user) but the provided one shares all of the required one" in  new Context {

      checkOR(ok = true, required_scopes = List("hello.world.test", "hello.share"), user_scopes = List("hello"))

    }



    "[AND] block when several scopes are required and several scope is provided (user) but the provided one doesn't share any part of the required one" in  new Context {

      checkAND(ok = false, required_scopes = List("hello.world.test", "world"), user_scopes = List("admin.world", "o.world.test"))

    }

    "[AND] block when several scopes are required and several scope is provided (user) but the provided one shares a part of the required one" in  new Context {

      checkAND(ok = false, required_scopes = List("hello.world.test", "world", "tom.jerry"), user_scopes = List("hello.world", "tom"))

    }

    "[AND] allow when several scopes are required and several scope is provided (user) but the provided one shares all of the required one" in  new Context {

      checkAND(ok = true, required_scopes = List("hello.world.test", "world.world"), user_scopes = List("hello", "world.world"))

    }


    "[OR] block when several scopes are required and several scope is provided (user) but the provided one doesn't share any part of the required one" in  new Context {

      checkOR(ok = false, required_scopes = List("hello.world.test", "world"), user_scopes = List("admin.world", "tom"))

    }

    "[OR] allow when several scopes are required and several scope is provided (user) but the provided one shares a part of the required one" in  new Context {

      checkOR(ok = true, required_scopes = List("hello.world.test", "hello", "tom.jerry"), user_scopes = List("hello.world", "tom.jerry"))

    }

    "[OR] allow when several scopes are required and several scope is provided (user) but the provided one shares all of the required one" in  new Context {

      checkOR(ok = true, required_scopes = List("hello.world.test", "hello.share"), user_scopes = List("hello.world", "hello.share"))

    }



  }






}
