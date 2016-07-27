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


}
