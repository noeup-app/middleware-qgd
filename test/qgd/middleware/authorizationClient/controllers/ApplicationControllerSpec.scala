package qgd.middleware.authorizationClient.controllers

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{ Environment, LoginInfo }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import net.codingwell.scalaguice.ScalaModule
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import qgd.middleware.authorizationClient.controllers.application.Applications
import qgd.middleware.models.Account

/**
 * Test case for the [[Applications]] class.
 */
class ApplicationControllerSpec extends PlaySpecification with Mockito {
  sequential

  "The `index` action" should {
    "redirect to login page if user is unauthorized" in new Context {
      new WithApplication(application) {
        val Some(redirectResult) = route(FakeRequest(qgd.middleware.authorizationClient.controllers.application.routes.Applications.index())
          .withAuthenticator[CookieAuthenticator](LoginInfo("invalid", "invalid"))
        )

        status(redirectResult) must be equalTo SEE_OTHER

        val redirectURL = redirectLocation(redirectResult).getOrElse("")
        redirectURL must contain(qgd.middleware.authorizationClient.controllers.login.routes.Logins.loginAction().toString)

        val Some(unauthorizedResult) = route(FakeRequest(GET, redirectURL))

        status(unauthorizedResult) must be equalTo OK
        contentType(unauthorizedResult) must beSome("text/html")
        contentAsString(unauthorizedResult) must contain("Silhouette - Sign In")
      }
    }

    "return 200 if user is authorized" in new Context {
      new WithApplication(application) {
        val Some(result) =
          identity.loginInfo match {
            case Some(loginInfo) =>
              route(FakeRequest(qgd.middleware.authorizationClient.controllers.application.routes.Applications.index())
                .withAuthenticator[CookieAuthenticator](loginInfo))
            case None => throw new RuntimeException("identity.loginInfo is None")
          }


        status(result) must beEqualTo(OK)
      }
    }
  }

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
      }
    }

    /**
     * An identity.
     */
    val identity = Account(
      id = UUID.randomUUID(),
      loginInfo = Some(LoginInfo("facebook", "user@facebook.com")),
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

    /**
     * The application.
     */
    lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .build()
  }
}
