package com.noeupapp.middleware.evolution

import play.api.mvc.{Action, Controller, Result}
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.Future
import anorm._
import play.api.Logger

import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 27/07/2016.
  */
class Evolution extends Controller {

  def apply(id: Int) = Action.async {
    id match {
      case 1 => _1
      case 2 => _2
      case 3 => _3
      case _ => Future.successful(NotFound)
    }
  }


  private def applyHelper(sql: String): Future[Result] = {
    TryBDCall { implicit c =>
      \/-(SQL(sql).execute())
    } map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("InternalServerError")
      case \/-(_) => Ok("OK")
    }
  }

  def _1 =
    applyHelper(
      """
        |ALTER TABLE public.auth_access_tokens ALTER COLUMN client_id SET NOT NULL;
        |
        |ALTER TABLE public.auth_access_tokens
        |ADD CONSTRAINT auth_access_tokens_auth_clients_client_id_fk
        |FOREIGN KEY (client_id) REFERENCES auth_clients (client_id);
        |
        |
        |ALTER TABLE public.auth_access_tokens
        |ADD CONSTRAINT auth_access_tokens_entity_users_id_fk
        |FOREIGN KEY (user_uuid) REFERENCES entity_users (id);
        |
        |ALTER TABLE public.entity_users ADD owned_by_client TEXT NULL;
        |
        |ALTER TABLE public.entity_users
        |ADD CONSTRAINT entity_users_auth_clients_client_id_fk
        |FOREIGN KEY (owned_by_client) REFERENCES auth_clients (client_id);
      """.stripMargin)

  def _2 =
    applyHelper(
      """
        |ALTER TABLE entity_users ADD created TIMESTAMP DEFAULT now() NOT NULL;
        |ALTER TABLE entity_organisations ADD created TIMESTAMP DEFAULT now() NOT NULL;
      """.stripMargin)

  def _3 =
    applyHelper(
      """
        |CREATE TABLE public.pass_hashes
        |(
        |  "user" UUID PRIMARY KEY,
        |  hasher TEXT NOT NULL,
        |  hash TEXT NOT NULL,
        |  salt TEXT,
        |  last_modified TIMESTAMP DEFAULT now() NOT NULL,
        |  CONSTRAINT pass_hashes_entity_users_id_fk FOREIGN KEY ("user") REFERENCES entity_users (id)
        |);
      """.stripMargin)

}
