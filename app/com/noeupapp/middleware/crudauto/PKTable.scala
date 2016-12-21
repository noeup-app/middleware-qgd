package com.noeupapp.middleware.crudauto


import slick.driver._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.lifted.PrimaryKey


trait PKTable{
  def pk: PrimaryKey
}
