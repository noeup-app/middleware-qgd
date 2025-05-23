package com.noeupapp.middleware.crudauto

import java.util.UUID

import akka.util.Timeout

import scala.concurrent.duration._
import anorm._
import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.{FakeScopeAndRoleAuthorization, ScopeAndRoleAuthorization}
import com.noeupapp.middleware.crudauto.model._
import com.noeupapp.testhelpers.{Context, FakeRequestBuilderOK}
import org.specs2.mock.Mockito
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import com.noeupapp.middleware.crudauto.model.Test._
import com.noeupapp.middleware.crudauto.model.Thing._
import com.noeupapp.middleware.crudauto.model.RelTestThing._
import com.noeupapp.middleware.entities.account.Account
import net.codingwell.scalaguice.ScalaModule
import com.mohiva.play.silhouette.test._


class CrudAutoSpec extends PlaySpecification with Mockito {

  override implicit def defaultAwaitTimeout: Timeout = 20.seconds

  "crud auto find all" should {
    "with empty table" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(GET, "/tests"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.arr()

      }
    }
    "with not empty table" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val Some(result) =
          route(FakeRequest(GET, "/tests"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) => elems must haveSize(2)
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
    "with not empty table and deleted elements" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({
          case h :: tail => h.copy(deleted = true) :: tail
        }))


        val Some(result) =
          route(FakeRequest(GET, "/tests"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) => elems must haveSize(1)
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
    "count returns 0 if table is empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val tests = await(allTests)

        val Some(result) =
          route(FakeRequest(GET, "/tests?count=true"))

        status(result) must be equalTo OK

        contentAsJson(result) must be equalTo Json.toJson(tests.length)
      }
    }
    "count returns n if table is not empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(GET, "/tests?count=true"))

        status(result) must be equalTo OK

        contentAsJson(result) must be equalTo Json.toJson(0)
      }
    }
    "pagination returns 0 element if table is empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(GET, "/tests?p=0&pp=5"))

        status(result) must be equalTo OK

        contentAsJson(result) must be equalTo Json.arr()
      }
    }
    "pagination returns n p=0 pp=5 element if table is not empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({tests =>
          {
            for {
              i <- 1 to 10
            } yield tests.map(_.copy(id = UUID.randomUUID()))
          }.flatten
        }))

        val tests = await(allTests)

        val Some(result) =
          route(FakeRequest(GET, "/tests?p=0&pp=5"))

        status(result) must be equalTo OK

        contentAsJson(result) match {
          case JsArray(elems) =>

            elems must haveSize(5)
//
//            sameElementsAs(
//              tests.slice(0, 5).map(Json.toJson(_)),
//              elems
//            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }
      }
    }
    "pagination returns n p=1 pp=5 element if table is not empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({tests =>
          {
            for {
              i <- 1 to 10
            } yield tests.map(_.copy(id = UUID.randomUUID()))
          }.flatten
        }))

        val tests = await(allTests)

        val Some(result) =
          route(FakeRequest(GET, "/tests?p=1&pp=5"))

        status(result) must be equalTo OK

        contentAsJson(result) match {
          case JsArray(elems) =>

            elems must haveSize(5)

//            sameElementsAs(
//              tests.slice(5, 10).map(Json.toJson(_)),
//              elems
//            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }
      }
    }
    "with not empty table and omit 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val Some(result) =
          route(FakeRequest(GET, "/tests?omit=id"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>
            elems must haveSize(2)

            sameElementsAs(
              testsBefore.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
    "with not empty table and omit several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val Some(result) =
          route(FakeRequest(GET, "/tests?omit=id,name"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>
            elems must haveSize(2)

            sameElementsAs(
              testsBefore.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id").-("name")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
    "with not empty table and require 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val Some(result) =
          route(FakeRequest(GET, "/tests?include=name"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>
            elems must haveSize(2)

            sameElementsAs(
              testsBefore.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id").-("typeL").-("priority")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
    "with not empty table and require several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val Some(result) =
          route(FakeRequest(GET, "/tests?include=name,typeL"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>
            elems must haveSize(2)

            sameElementsAs(
              testsBefore.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id").-("priority")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }
    }
  }

  "crud auto deep find all" should {
    "return not found if first entity does not exists" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${UUID.randomUUID()}/things"))

        status(result) must be equalTo NOT_FOUND
      }

    }
    "return not found of entity designed by id is deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate(_.map{
          case t if t.id == pk => t.copy(deleted = true)
          case t => t
        }))

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things"))

        status(result) must be equalTo NOT_FOUND

      }

    }
    "return a list of things" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>

            sameElementsAs(
              expected.map{ t =>
                Json.toJson(t)
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }

    }
    "return a list of things and omit 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things?omit=id"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>

            sameElementsAs(
              expected.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }

    }
    "return a list of things and omit several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things?omit=id,name"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>

            sameElementsAs(
              expected.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("id").-("name")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }

    }
    "return a list of things and require 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things?include=id"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>

            sameElementsAs(
              expected.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("test").-("name")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }

    }
    "return a list of things and require several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val expected = await(allThings)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk/things?include=id,name"))

        status(result) must be equalTo OK


        contentAsJson(result) match {
          case JsArray(elems) =>

            sameElementsAs(
              expected.map{ t =>
                Json.toJson(t).asInstanceOf[JsObject].-("test")
              },
              elems
            ) must beTrue
          case json =>
            (false must beTrue).setMessage(s"Unexpected json : $json")
        }

      }

    }
  }

  "crud auto deep find by id" should {
    "return not found if first entity does not exists" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests)

        val test = tests.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${UUID.randomUUID()}"))

        status(result) must be equalTo NOT_FOUND
      }

    }
    "return not found if first entity is deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate({
          case h :: tail => h.copy(deleted = true) :: tail
        }))

        val tests = await(allTests)

        val test = tests.find(_.deleted == true).get

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${UUID.randomUUID()}"))

        status(result) must be equalTo NOT_FOUND
      }

    }
    "return a 404 if second entity does not exists" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests)
        val things = await(allThings)

        val test = tests.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${UUID.randomUUID()}"))

        status(result) must be equalTo NOT_FOUND
      }

    }
    "return a thing" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests).map(toTestOut)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${thing.id}"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.toJson(thing)
      }

    }
    "return a thing and omit 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests).map(toTestOut)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${thing.id}?omit=id"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.toJson(thing).asInstanceOf[JsObject].-("id")
      }

    }
    "return a thing and omit several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests).map(toTestOut)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${thing.id}?omit=id,test"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.toJson(thing).asInstanceOf[JsObject].-("id").-("test")
      }

    }
    "return a thing and require 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests).map(toTestOut)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${thing.id}?include=id"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.toJson(thing).asInstanceOf[JsObject].-("test").-("name")
      }

    }
    "return a thing and require several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)
        await(populate)

        val tests = await(allTests).map(toTestOut)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${test.id}/things/${thing.id}?include=id,test"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.toJson(thing).asInstanceOf[JsObject].-("name")
      }

    }
  }


  "crud auto find by id" should {
    "return not found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(GET, "/tests/" + UUID.randomUUID()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return not found if model is deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({
          case h :: tail => h.copy(deleted = true) :: tail
        }))

        val testsBefore = await(allTests)

        val test = testsBefore.find(_.deleted == true).get

        val Some(result) =
          route(FakeRequest(GET, "/tests/" + test.id))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return 200 if model designed by id is found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val Some(result) =
          route(FakeRequest(GET, s"/tests/$pk"))

        status(result) must be equalTo OK
        contentAsJson(result).as[TestOut] must be equalTo testsBefore.find(_.id == pk).get

      }
    }
    "return 200 if model designed by id is found and omit 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toGet = testsBefore.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${toGet.id}?omit=name"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.obj(
          "id" -> toGet.id,
          "typeL" -> toGet.typeL,
          "priority" -> toGet.priority
        )
      }
    }
    "return 200 if model designed by id is found and omit several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toGet = testsBefore.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${toGet.id}?omit=name,typeL"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.obj(
          "id" -> toGet.id,
          "priority" -> toGet.priority
        )
      }
    }
    "return 200 if model designed by id is found and include 1 field" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toGet = testsBefore.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${toGet.id}?include=priority"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.obj(
          "priority" -> toGet.priority
        )
      }
    }
    "return 200 if model designed by id is found and include several fields" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toGet = testsBefore.head

        val Some(result) =
          route(FakeRequest(GET, s"/tests/${toGet.id}?include=priority,id"))

        status(result) must be equalTo OK
        contentAsJson(result) must be equalTo Json.obj(
          "id" -> toGet.id,
          "priority" -> toGet.priority
        )
      }
    }
  }


  "crud auto delete" should {
    "return not found" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(DELETE, "/tests/" + UUID.randomUUID()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return 200 if entities depend on what we want to purge delete" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore = await(allTests)
        val thingsBefore = await(allThings)

        val test = testsBefore.find(t => thingsBefore.map(_.test).contains(t.id)).get

        val Some(result) =
          route(FakeRequest(DELETE, "/tests/" + test.id))

        status(result) must be equalTo OK

      }
    }
    "return 500 if entities depend on what we want to delete" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore = await(allTests)
        val thingsBefore = await(allThings)

        val test = testsBefore.find(t => thingsBefore.map(_.test).contains(t.id)).get

        val Some(result) =
          route(FakeRequest(DELETE, s"/tests/${test.id}?force_delete=true"))

        status(result) must be equalTo INTERNAL_SERVER_ERROR

      }
    }

    "return not found if model is deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({
          case h :: tail => h.copy(deleted = true) :: tail
        },{ things =>
          Seq.empty
        }))

        val testsBefore: Seq[Test] = await(allTests)

        val test = testsBefore.find(_.deleted == true).get

        val Some(result) =
          route(FakeRequest(DELETE, "/tests/" + test.id))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "return 200 if model designed by id is found and contains field deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate(identity[Seq[Test]], _ => Seq.empty))

        val testsBefore: Seq[Test] = await(allTests)

        val toDelete = testsBefore.head

        val Some(result) =
          route(FakeRequest(DELETE, s"/tests/${toDelete.id}"))


        status(result) must be equalTo OK

        val testsAfter: Seq[Test] = await(allTests)

        testsAfter.find(_.id == toDelete.id).get.deleted must beTrue

      }
    }
    "return 200 if model designed by id is found, contains field deleted and ask to force delete" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate(identity[Seq[Test]], _ => Seq.empty))

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val toDelete = testsBefore.head

        val Some(result) =
          route(FakeRequest(DELETE, s"/tests/${toDelete.id}?force_delete=true"))


        status(result) must be equalTo OK

        val testsAfter: Seq[TestOut] = await(allTests).map(toTestOut)

        testsBefore.filterNot(_.id == toDelete.id) must be equalTo testsAfter

      }
    }
    "return 200 if model designed by id is found, does not contains field deleted and ask" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val thingsBefore = await(allThings).map(toThingOut)

        val toDelete = thingsBefore.head

        val Some(result) =
          route(FakeRequest(DELETE, s"/things/${toDelete.id}"))


        status(result) must be equalTo OK

        val thingsAfter = await(allThings).map(toThingOut)

        thingsBefore.filterNot(_.id == toDelete.id) must be equalTo thingsAfter

      }
    }
  }


  "crud auto add" should {
    "work if input is correct" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val toAdd = TestIn("my super name", "my type", 12345)

        val Some(result) =
          route(FakeRequest(POST, "/tests")
                .withBody(Json.toJson(toAdd)))


        status(result) must be equalTo OK

        val testsAfter: Seq[TestOut] = await(allTests).map(toTestOut)

        sameElementsAs(
          contentAsJson(result).as[TestOut] :: testsBefore.toList,
          testsAfter
        ) must beTrue

      }
    }
    "fail if input is not correct" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)


        val Some(result) =
          route(FakeRequest(POST, "/tests")
                .withBody(Json.obj(
                  "aldzùmlakzdùmlakzdùmazd" -> "mlazdlkùazdùmlkazmùldkùmalzd"
                )))

        status(result) must be equalTo BAD_REQUEST

      }
    }
    "work with auto increment" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val tests  = await(allTests)
        val things = await(allThings)

        val test = tests.head
        val thing = things.head

        val relBefore = await(allRel).toList

        val Some(result) =
          route(FakeRequest(POST, "/rel")
                .withBody(Json.toJson(RelTestThingIn(test.id, thing.id))))

        val relAfter = await(allRel)

        val expectedId = relBefore.map(_.id).max + 1


        status(result) must be equalTo OK

        contentAsJson(result).as[RelTestThing] must be equalTo RelTestThing(expectedId, test.id, thing.id)

        sameElementsAs(
            RelTestThing(expectedId, test.id, thing.id) :: relBefore,
            relAfter
        ) must beTrue


      }
    }
  }


  "crud auto update" should {
    "fail if entity does not exists" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        val Some(result) =
          route(FakeRequest(PUT, "/tests/" + UUID.randomUUID())
            .withBody(Json.obj()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "fail if entity is deleted" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate({
          case h :: tail => h.copy(deleted = true) :: tail
        }))

        val testsBefore: Seq[Test] = await(allTests)

        val test = testsBefore.find(_.deleted == true).get

        val Some(result) =
          route(FakeRequest(PUT, "/tests/" + test.id)
            .withBody(Json.obj()))

        status(result) must be equalTo NOT_FOUND

      }
    }
    "work if input is correct" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[TestOut] = await(allTests).map(toTestOut)

        val toUpdate = testsBefore.head
        val updated = toUpdate.copy(name = "my super new name", typeL = "my wonderful new type")

        val Some(result) =
          route(FakeRequest(PUT, s"/tests/${toUpdate.id}")
                .withBody(Json.obj(
                  "name" -> updated.name,
                  "typeL" -> updated.typeL
                )))


        status(result) must be equalTo OK

        val testsAfter: Seq[TestOut] = await(allTests).map(toTestOut)

        contentAsJson(result).as[TestOut] must be equalTo updated

        sameElementsAs(
          contentAsJson(result).as[TestOut] :: testsBefore.toList.filterNot(_.id == updated.id),
          testsAfter
        ) must beTrue

      }
    }
    "fail if input is not correct 1" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toUpdate = testsBefore.head
        val updated = toUpdate.copy(name = "my super new name", typeL = "my wonderful new type")

        val Some(result) =
          route(FakeRequest(PUT, s"/tests/${toUpdate.id}")
                .withBody(Json.obj(
                  "aldzùmlakzdùmlakzdùmazd" -> "mlazdlkùazdùmlkazmùldkùmalzd"
                )))

        status(result) must be equalTo BAD_REQUEST

      }
    }
    "fail if input is not correct 2" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toUpdate = testsBefore.head
        val updated = toUpdate.copy(name = "my super new name", typeL = "my wonderful new type")

        val Some(result) =
          route(FakeRequest(PUT, s"/tests/${toUpdate.id}")
            .withBody(Json.obj(
              "name" -> updated.name,
              "typeL" -> updated.typeL,
              "aldzùmlakzdùmlakzdùmazd" -> "mlazdlkùazdùmlkazmùldkùmalzd"
            )))

        status(result) must be equalTo BAD_REQUEST

      }
    }
    "fail if input is not empty" in new CrudAutoContext {
      new WithApplication(application) {

        await(createTables)

        await(populate)

        val testsBefore: Seq[Test] = await(allTests)

        val toUpdate = testsBefore.head
        val updated = toUpdate.copy(name = "my super new name", typeL = "my wonderful new type")

        val Some(result) =
          route(FakeRequest(PUT, s"/tests/${toUpdate.id}")
            .withBody(Json.obj()))

        status(result) must be equalTo BAD_REQUEST

      }
    }
  }


  "crud auto with generic type of Id" should {
    "works using find by id" in new CrudAutoContext {
      new WithApplication(application) {
        await(createTables)
        await(populate)

        val rels = await(allRel)

        val rel = rels.head

        val Some(result) =
          route(FakeRequest(GET, "/rel/" + rel.id))

        status(result) must be equalTo OK
      }
    }


    "return bad request using find by id if UUID is given" in new CrudAutoContext {
      new WithApplication(application) {
        await(createTables)
        await(populate)

        val rels = await(allRel)

        val rel = rels.head

        val Some(result) =
          route(FakeRequest(GET, "/rel/" + UUID.randomUUID()))

        status(result) must be equalTo BAD_REQUEST
      }
    }

    //    "fail if entity is deleted" in new CrudAutoContext {
    //      new WithApplication(application) {
    //
    //        await(createTables)
    //
    //        await(populate({
    //          case h :: tail => h.copy(deleted = true) :: tail
    //        }))
    //
    //        val testsBefore: Seq[Test] = await(allTests)
    //
    //        val test = testsBefore.find(_.deleted == true).get
    //
    //        val Some(result) =
    //          route(FakeRequest(PUT, "/rel/" + test.id)
    //            .withBody(Json.obj()))
    //
    //        status(result) must be equalTo NOT_FOUND
    //
    //      }
    //    }
  }

  "crud auto authorization" should {
    "return unauthorized by default if user is not connected" in new CustomizableCrudAutoContext {

      override def provideCrudClassName: CrudClassName = new CrudClassName {
        override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
          "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
        )
      }

      new WithApplication(application) {
        val Some(resultFindAll) =
          route(FakeRequest(GET, "/authtests"))

        status(resultFindAll) must be equalTo UNAUTHORIZED


        val Some(resultFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd"))

        status(resultFindById) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindAll) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd"))

        status(resultdeepFindAll) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd/azdazd"))

        status(resultdeepFindById) must be equalTo UNAUTHORIZED


        val Some(resultAdd) =
          route(FakeRequest(POST, "/authtests")
                  .withBody(Json.obj()))

        status(resultAdd) must be equalTo UNAUTHORIZED


        val Some(resultUpdate) =
          route(FakeRequest(PUT, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultUpdate) must be equalTo UNAUTHORIZED


        val Some(resultDelete) =
          route(FakeRequest(DELETE, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultDelete) must be equalTo UNAUTHORIZED
      }

    }
    "return 200 by default if user is connected" in new CustomizableCrudAutoContext {

      override def provideCrudClassName: CrudClassName = new CrudClassName {
        override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
          "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
            .withCustomAuth(newFindAll = OpenAccess)
        )
      }

      new WithApplication(application) {

        val Some(resultFindAll) =
          route(FakeRequest(GET, "/authtests")
            .withAuthenticator[BearerTokenAuthenticator](loginInfo))

        status(resultFindAll) must be equalTo OK


        val Some(resultFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd"))

        status(resultFindById) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindAll) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd"))

        status(resultdeepFindAll) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd/azdazd"))

        status(resultdeepFindById) must be equalTo UNAUTHORIZED


        val Some(resultAdd) =
          route(FakeRequest(POST, "/authtests")
                  .withBody(Json.obj()))

        status(resultAdd) must be equalTo UNAUTHORIZED


        val Some(resultUpdate) =
          route(FakeRequest(PUT, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultUpdate) must be equalTo UNAUTHORIZED


        val Some(resultDelete) =
          route(FakeRequest(DELETE, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultDelete) must be equalTo UNAUTHORIZED
      }

    }
    "return 200 if the configuration is set to withOpenAccess" in new CustomizableCrudAutoContext {

      override def provideCrudClassName: CrudClassName = new CrudClassName {
        override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
          "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
              .withCustomAuth(newFindAll = OpenAccess)
        )
      }

      new WithApplication(application) {

        await(createTables)

        val Some(resultFindAll) =
          route(FakeRequest(GET, "/authtests"))

        status(resultFindAll) must be equalTo OK


        val Some(resultFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd"))

        status(resultFindById) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindAll) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd"))

        status(resultdeepFindAll) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd/azdazd"))

        status(resultdeepFindById) must be equalTo UNAUTHORIZED


        val Some(resultAdd) =
          route(FakeRequest(POST, "/authtests")
            .withBody(Json.obj()))

        status(resultAdd) must be equalTo UNAUTHORIZED


        val Some(resultUpdate) =
          route(FakeRequest(PUT, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultUpdate) must be equalTo UNAUTHORIZED


        val Some(resultDelete) =
          route(FakeRequest(DELETE, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultDelete) must be equalTo UNAUTHORIZED
      }

    }
    "return 200 if the configuration is set to withOpenAccess and role is OK" in new CustomizableCrudAutoContext {

      override def provideCrudClassName: CrudClassName = new CrudClassName {
        override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
          "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
            .withCustomAuth(newFindAll = SecuredAccessWithRole("admin"))
        )
      }

      override def provideRoleList: List[String] = List("admin")


      new WithApplication(application) {

        await(createTables)

        val Some(resultFindAll) =
          route(FakeRequest(GET, "/authtests")
            .withAuthenticator(id.loginInfo))

        status(resultFindAll) must be equalTo OK


        val Some(resultFindById) =
          route(FakeRequest(GET, "/authtests/azadazd"))

        status(resultFindById) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindAll) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd"))

        status(resultdeepFindAll) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd/azdazd"))

        status(resultdeepFindById) must be equalTo UNAUTHORIZED


        val Some(resultAdd) =
          route(FakeRequest(POST, "/authtests")
            .withBody(Json.obj()))

        status(resultAdd) must be equalTo UNAUTHORIZED


        val Some(resultUpdate) =
          route(FakeRequest(PUT, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultUpdate) must be equalTo UNAUTHORIZED


        val Some(resultDelete) =
          route(FakeRequest(DELETE, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultDelete) must be equalTo UNAUTHORIZED
      }

    }

    "return unauthorized if the configuration is set to withOpenAccess and the role is KO" in new CustomizableCrudAutoContext {

      override def provideCrudClassName: CrudClassName = new CrudClassName {
        override def configure: Map[String, CrudConfiguration[_, _, _]] = Map(
          "authtests"    -> configuration[AuthTest, Long, AuthTestTableDef]
            .withCustomAuth(newFindAll = SecuredAccessWithRole("admin"))
        )
      }

      override def provideRoleList: List[String] = List()


      new WithApplication(application) {

        await(createTables)

        val Some(resultFindAll) =
          route(FakeRequest(GET, "/authtests"))

        status(resultFindAll) must be equalTo UNAUTHORIZED


        val Some(resultFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd"))

        status(resultFindById) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindAll) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd"))

        status(resultdeepFindAll) must be equalTo UNAUTHORIZED


        val Some(resultdeepFindById) =
          route(FakeRequest(GET, "/authtests/azdklmjklmazd/azdazd/azdazd"))

        status(resultdeepFindById) must be equalTo UNAUTHORIZED


        val Some(resultAdd) =
          route(FakeRequest(POST, "/authtests")
            .withBody(Json.obj()))

        status(resultAdd) must be equalTo UNAUTHORIZED


        val Some(resultUpdate) =
          route(FakeRequest(PUT, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultUpdate) must be equalTo UNAUTHORIZED


        val Some(resultDelete) =
          route(FakeRequest(DELETE, "/authtests/azdazd")
            .withBody(Json.obj()))

        status(resultDelete) must be equalTo UNAUTHORIZED
      }

    }
  }


}
