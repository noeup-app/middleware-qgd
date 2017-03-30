package com.noeupapp.middleware.crudauto

import java.util.UUID

import com.noeupapp.middleware.entities.user.{User, UserTableDef}
import com.noeupapp.middleware.files.{File, FileTableDef}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._

/**
  * Created by damien on 30/03/2017.
  */
object MiddleCrudAutoConfiguration extends CrudClassName {

  override def configure: Map[String, CrudConfiguration[_, _, _]] = Map (
    "users" -> configuration[User,  UUID, UserTableDef],
    "files" -> configuration[File,  UUID, FileTableDef]
  )

}
