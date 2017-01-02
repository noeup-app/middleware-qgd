package com.noeupapp.middleware.authorizationClient.provider

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue

import scala.concurrent.Future

class QGDProvider (
                   protected val httpLayer: HTTPLayer,
                   protected val stateProvider: OAuth2StateProvider,
                   val settings: OAuth2Settings)
  extends OAuth2Provider with CommonSocialProfileBuilder {

  override type Self = QGDProvider

  override def withSettings(f: (OAuth2Settings) => OAuth2Settings): Self = new QGDProvider(httpLayer, stateProvider, f(settings))

  override protected def profileParser: SocialProfileParser[JsValue, Profile] = new QGDProfileParser

  /**
    * The content type to parse a profile from.
    */
  override type Content = JsValue

  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    Logger.error("TODO : buildProfile " + authInfo)
    Future(CommonSocialProfile(LoginInfo("QGD TEST", "qgd")))
  }

  override protected def urls: Map[String, String] = Map()

  /**
    * The provider ID.
    */
  override def id: String = QGDProvider.ID
}

class QGDProfileParser extends SocialProfileParser[JsValue, CommonSocialProfile] {
  override def parse(content: JsValue): Future[CommonSocialProfile] = {
    Logger.error("TODO : parse " + content.toString())
    Future(CommonSocialProfile(LoginInfo("QGD TEST", "qgd")))
  }
}

object QGDProvider {

  /**
    * The error messages.
    */
  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error code: %s, message: %s"

  /**
    * The Google constants.
    */
  val ID = "qgd"
  val API = "http://localhost:9000/"
}
