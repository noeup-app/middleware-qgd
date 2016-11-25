package com.noeupapp.middleware.crudauto

import java.util.UUID

import akka.util.Timeout

import scala.concurrent.duration._
import anorm._
import com.noeupapp.testhelpers.Context
import org.specs2.mock.Mockito
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsArray, Json}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import com.noeupapp.middleware.crudauto.Test._

class CrudAutoSpec extends PlaySpecification with Mockito {

  override implicit def defaultAwaitTimeout: Timeout = 20.seconds

  "crud auto find all" should {
    "with empty table" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        val Some(result) =
          route(FakeRequest("GET", "/tests"))

        status(result) must be equalTo OK


      }
    }
    "with not empty table" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        await(populate)

        val Some(result) =
          route(FakeRequest("GET", "/tests"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) => elems must haveSize(2)
          case json =>
            dropTable
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
  }

  "crud auto find by id" should {
    "return not found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        val Some(result) =
          route(FakeRequest(GET, "/tests/" + UUID.randomUUID()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return 200 if model designed by id is found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        await(populate)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk"))

        status(result) must be equalTo OK

      }
    }
  }
  "crud auto delete" should {
    "return not found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        val Some(result) =
          route(FakeRequest(DELETE, "/tests/" + UUID.randomUUID()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return 200 if model designed by id is found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        await(populate)

        val testsBefore: Seq[Test] = await(all)

        val toDelete = testsBefore.head

        val Some(result) =
          route(FakeRequest(DELETE, s"/tests/${toDelete.id}"))

        val testsAfter: Seq[Test] = await(all)

        status(result) must be equalTo OK
        testsBefore.filterNot(_.id == toDelete.id) must be equalTo testsAfter

      }
    }
  }

  "crud auto add" should {
    "work if input is correct" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)

        val toAdd = TestIn("my super name", "my type", 12345)

        val Some(result) =
          route(FakeRequest(POST, "/tests")
                .withBody(Json.toJson(toAdd)))

        status(result) must be equalTo OK

      }
    }
    "fail if input is not correct" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTable)


        val Some(result) =
          route(FakeRequest(POST, "/tests")
                .withBody(Json.obj(
                  "aldzùmlakzdùmlakzdùmazd" -> "mlazdlkùazdùmlkazmùldkùmalzd"
                )))

        status(result) must be equalTo BAD_REQUEST

      }
    }
  }


}
