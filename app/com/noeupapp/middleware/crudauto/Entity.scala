package com.noeupapp.middleware.crudauto

/**
  * Created by damien on 15/11/2016.
  */
abstract class Entity[T]{
  val id: T
  def withNewId(id: T): Entity[T]
}