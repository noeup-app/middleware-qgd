package com.noeupapp.middleware.crudauto


import slick.driver._
import slick.driver.PostgresDriver.api._

/**
  * Created by damien on 16/11/2016.
  */
trait PKTable[PK]{
  def id: Rep[PK]
}
