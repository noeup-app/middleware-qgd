package com.noeupapp.middleware.utils

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}

case class SendinBlueConfig(baseUrl: String, apiKey:String)

class SMS @Inject()(ws: WSClient,
                    sendinBlueConfig: SendinBlueConfig){

  def sendSMS(phone: String, text: String, from: String): Future[Expect[JsValue]] = {
    val content = "{\"text\":\""+text+"\",\"from\":\""+from+"\",\"to\":\""+phone+"\",\"type\":\"transactional\"}"
    Logger.debug(content)
    val request: WSRequest = ws.url(sendinBlueConfig.baseUrl+"sms")
    val complexRequest: WSRequest =
      request
        .withMethod("POST")
        .withHeaders(("api-key",sendinBlueConfig.apiKey),
          ("Content-Type","application/json"))
        .withBody(content)

    val futureResponse: Future[(WSResponse)] =
      complexRequest.execute()
    futureResponse.map{response =>
      response.status match {
        case it if it >= 200 && it <= 299 => \/-(response.json)

        case _ => -\/(FailError("Couldn't send SMS"))
      }
    }
  }
}
