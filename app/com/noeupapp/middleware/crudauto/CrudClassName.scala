package com.noeupapp.middleware.crudauto

/**
  * Created by damien on 15/11/2016.
  */
trait CrudClassName {
  def getClassNames(modelName: String): Option[String]
}
