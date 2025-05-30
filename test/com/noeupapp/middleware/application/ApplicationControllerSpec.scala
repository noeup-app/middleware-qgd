package com.noeupapp.middleware.application

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{Environment, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.mohiva.play.silhouette.test._
import com.noeupapp.middleware.authorizationClient.{FakeScopeAndRoleAuthorization, ScopeAndRoleAuthorization}
import com.noeupapp.testhelpers.Context
import net.codingwell.scalaguice.ScalaModule
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

/**
 * Test case for the [[Applications]] class.
 */
class ApplicationControllerSpec extends PlaySpecification with Mockito {
  sequential

  "The `index` action" should {
    "redirect to login page if user is unauthorized" in new Context {
      new WithApplication(application) {
        val Some(redirectResult) = route(FakeRequest(com.noeupapp.middleware.application.routes.Applications.index())
          .withAuthenticator[BearerTokenAuthenticator](LoginInfo("invalid", "invalid"))
        )

        status(redirectResult) must be equalTo SEE_OTHER

        val redirectURL = redirectLocation(redirectResult).getOrElse("")
        redirectURL must contain(com.noeupapp.middleware.authorizationClient.login.routes.Logins.loginAction().toString)

        val Some(unauthorizedResult) = route(FakeRequest(GET, redirectURL))

        status(unauthorizedResult) must be equalTo OK
        contentType(unauthorizedResult) must beSome("text/html")
        contentAsString(unauthorizedResult) must contain("Silhouette - Sign In")
      }
    }

    "return 200 if user is authorized" in new Context {
      new WithApplication(application) {
        val Some(result) =
          route(FakeRequest(com.noeupapp.middleware.application.routes.Applications.index())
                .withAuthenticator[BearerTokenAuthenticator](identity.loginInfo))


        status(result) must beEqualTo(OK)
      }
    }
  }

//  /**
//   * The context.
//   */
//  trait Context extends Scope {
//
//    /**
//     * A fake Guice module.
//     */
//    class FakeModule extends AbstractModule with ScalaModule {
//      def configure() = {
//        bind[Environment[Account, BearerTokenAuthenticator]].toInstance(env)
//        bind[ScopeAndRoleAuthorization].to[FakeScopeAndRoleAuthorization]
//      }
//    }
//
//    /**
//     * An identity.
//     */
//    val identity = Account(
//      id = UUID.randomUUID(),
//      loginInfo = Some(LoginInfo("facebook", "user@facebook.com")),
//      firstName = None,
//      lastName = None,
//      fullName = None,
//      email = None,
//      scopes = List(),
//      roles = List(),
//      avatarURL = None,
//      deleted = false
//    )
//
//    /**
//     * A Silhouette fake environment.
//     */
//    implicit val env: Environment[Account, BearerTokenAuthenticator] = {
//      identity.loginInfo match {
//        case Some(li) =>
//          new FakeEnvironment[Account, BearerTokenAuthenticator](Seq(li -> identity))
//        case None => throw new RuntimeException("identity.loginInfo is None")
//      }
//
//    }
//
//    /**
//     * The application.
//     */
//    lazy val application = new GuiceApplicationBuilder()
//      .overrides(new FakeModule)
//      .build()
//  }
}
