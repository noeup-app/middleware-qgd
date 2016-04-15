package com.noeupapp.middleware.authorizationClient.controllers

import com.noeupapp.testhelper.Context
import org.specs2.mock.Mockito
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}


class OAuth2Spec extends PlaySpecification with Mockito {

  "issue access token" should {
    "works with grant type password" in new Context {
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(com.noeupapp.middleware.authorizationServer.oauth2.routes.OAuth2s.accessToken()))

        status(result) must be equalTo OK
        contentType(result) must beSome("application/json")
        //      contentAsString(result) must contain("Silhouette - Sign In")
      }
    }
  }


}
