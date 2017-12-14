package com.noeupapp.middleware.crudauto

import slick.lifted.PrimaryKey


trait PKTable{
  def pk: PrimaryKey
}
