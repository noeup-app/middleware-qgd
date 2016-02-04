package qgd.authorizationClient.controllers

import java.util.UUID
import javax.inject.Inject

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Silhouette, Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.mohiva.play.silhouette.test._
import play.api.i18n.MessagesApi
import qgd.authorizationClient.models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import qgd.authorizationClient.results.{AjaxAuthorizationResult, HtmlScalaViewAuthorizationResult}
import qgd.authorizationClient.utils.{WithScopes, WithScope}

class ScopeSpec extends PlaySpecification with Mockito {
  sequential

  "Scope filter" should {
    "allow when no scope is required and when user has no scope" in new ContextWithoutScope {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testNoScopeRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }
    "allow when no scope is required and when user has scope" in new ContextWithoutScope {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testNoScopeRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

    "filter when one scope is required and when user has no scope" in new ContextWithoutScope {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must be equalTo SEE_OTHER
      }
    }


    "filter when one scope is required and when user has one but different scope" in new ContextWithScopeAdmin {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeBuyRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must be equalTo SEE_OTHER
      }
    }

    "allow when one scope is required and when user has several scopes including the right one" in new ContextWithScopeAdminAndBuy  {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeBuyRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

    "allow when one scope is required and when user has the right one" in new ContextWithScopeAdmin {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

    "allow when one of two scopes is required and when user has the right one" in new ContextWithScopeAdmin {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

    "allow when one of two scopes is required and when user has both" in new ContextWithScopeAdminAndBuy  {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminOrBuyRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

    "filter when two scopes are required and when user has only one" in new ContextWithScopeAdmin {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminAndBuyRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must be equalTo SEE_OTHER
      }
    }

    "filter when two scopes are required and when user has both" in new ContextWithScopeAdminAndBuy  {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.TestScopeController.testScopeAdminAndBuyRequired())
          .withAuthenticator[CookieAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }

  }




  /**
    * The contexts.
    */
  trait ContextWithoutScope extends Scope {

    /**
      * A fake Guice module.
      */
    class FakeModule extends AbstractModule with ScalaModule {
      def configure() = {
        bind[Environment[User, CookieAuthenticator]].toInstance(env)
      }
    }

    /**
      * An identity.
      */
    def identity = User(
      userID = UUID.randomUUID(),
      loginInfo = LoginInfo("facebook", "user@facebook.com"),
      firstName = None,
      lastName = None,
      fullName = None,
      email = None,
      scopes = List(),
      roles = List(),
      avatarURL = None
    )

    /**
      * A Silhouette fake environment.
      */
    implicit val env: Environment[User, CookieAuthenticator] = new FakeEnvironment[User, CookieAuthenticator](Seq(identity.loginInfo -> identity))

    /**
      * The application.
      */
    lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .build()
  }

  trait ContextWithScopeAdmin extends ContextWithoutScope{
    override def identity = super.identity.copy(scopes = List("admin"))
  }

  trait ContextWithScopeAdminAndBuy extends ContextWithoutScope{
    override def identity = super.identity.copy(scopes = List("admin", "buy"))
  }

}
