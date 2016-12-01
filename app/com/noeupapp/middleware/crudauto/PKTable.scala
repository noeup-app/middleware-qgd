package com.noeupapp.middleware.crudauto


import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.lifted.PrimaryKey


trait PKTable{
  def pk: PrimaryKey
}
