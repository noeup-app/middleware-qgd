package com.noeupapp.middleware.authorizationServer.oauth2

import com.noeupapp.testhelpers.Context
import org.specs2.mock.Mockito
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.DataPart
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}


class OAuth2Spec extends PlaySpecification with Mockito {

  "issue access token" should {
    "works with grant type password" in new Context {
      new WithApplication(application) {

        val data2post: Seq[(String, String)] = Seq(
          "client_id"     -> "client_id",
          "client_secret" -> "client_secret",
          "grant_type"    -> "password",
          "username"      -> "username",
          "password"      -> "password"
        )

//        val request = FakeRequest("GET", "/")
        val request = FakeRequest(com.noeupapp.middleware.authorizationServer.oauth2.routes.OAuth2s.accessToken())
//          .withFormUrlEncodedBody(data2post:_*)

        val Some(result) =
          route(
            request
          )

        println(contentAsString(result))
        status(result) must be equalTo OK
        contentType(result) must beSome("application/json")
      }
    }
  }


}
