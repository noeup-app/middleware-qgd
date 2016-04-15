package com.noeupapp.testhelpers

import com.google.inject.Inject
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.test._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest

class FakeRequestBuilder @Inject()() {

  def apply(httpMethod: String, url: String, data2post: Option[JsValue], oracle: JsValue)(implicit context: Context, spec: AbstractSpec) = {
    import context._
    import spec._
    val Some(result) = {
      data2post match {
        case Some(data) =>
          route(FakeRequest(httpMethod, url)
            .withAuthenticator[CookieAuthenticator](loginInfo)
            .withJsonBody(data))
        case None =>
          route(FakeRequest(httpMethod, url)
            .withAuthenticator[CookieAuthenticator](loginInfo))
      }
    }
    status(result) must be equalTo OK
    contentType(result) must beSome("application/json")
    contentAsString(result) must contain(Json.stringify(oracle))
  }

}
