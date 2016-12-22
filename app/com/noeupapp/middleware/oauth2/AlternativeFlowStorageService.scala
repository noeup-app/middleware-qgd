package com.noeupapp.middleware.oauth2


import com.google.inject.Inject
import com.noeupapp.middleware.authorizationServer.client.Client
import com.noeupapp.middleware.entities.user.UserOut
import com.noeupapp.middleware.entities.user.User.UserOutFormat
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.BearerTokenGenerator
import org.sedis.Pool
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by damien on 25/07/2016.
  */
class AlternativeFlowStorageService @Inject()(pool: Pool) {


  case class UserAndClient(user: UserOut, client: Client)

  implicit val UserAndClientFormat = Json.format[UserAndClient]

  def createAndStoreToken(user: UserOut, client: Client): Future[Expect[String]] = Future {
    val token = BearerTokenGenerator.generateToken
    Try{
      pool.withClient(_.set(
        token,
        Json.stringify(Json.toJson(UserAndClient(user, client))(UserAndClientFormat))
      ))
      token
    }
  }


  def checkToken(token: String): Future[Expect[Option[(UserOut, Client)]]] = Future {
    TryExpect{
      pool.withClient(_.get(token))
        .map(Json.parse)
        .map(u => Json.fromJson(u)(UserAndClientFormat)) match {
          case Some(JsSuccess(res, _)) => \/-(Some((res.user, res.client)))
          case Some(JsError(errors))   => -\/(FailError(s"Unable to parse user $errors"))
          case None                    => \/-(None)
        }
    }
  }


}
