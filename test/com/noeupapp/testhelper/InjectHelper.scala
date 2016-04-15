package com.noeupapp.testhelper

import play.api.inject.guice.GuiceApplicationBuilder

import scala.reflect.ClassTag

trait InjectHelper {
  lazy val injector = (new GuiceApplicationBuilder).injector()

  def inject[T : ClassTag]: T = injector.instanceOf[T]
}
