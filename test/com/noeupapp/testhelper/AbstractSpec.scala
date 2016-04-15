package com.noeupapp.testhelper

import org.specs2.mock.Mockito
import play.api.test.PlaySpecification

trait AbstractSpec extends PlaySpecification with Mockito {
  implicit val spec: AbstractSpec = this
}