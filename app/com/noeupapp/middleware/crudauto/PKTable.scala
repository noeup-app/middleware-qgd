package com.noeupapp.middleware.crudauto


import slick.driver._
import slick.driver.PostgresDriver.api._

/**
  * Created by damien on 16/11/2016.
  */
abstract class PKTable[E, PK](tag: Tag, tableName: String) extends Table[E](tag, tableName){
  def id: Rep[PK]
}
