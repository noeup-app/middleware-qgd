package com.noeupapp.testhelpers

import java.sql.Connection

import anorm.SqlParser._
import anorm._
import com.google.inject.Inject
import play.api.Play.current
import play.api.db.DB

class SqlHelper @Inject()() {


  def getNumberOfElementInTable(tableName: String)(implicit connection: Connection): Long = {
    DB.withConnection({ c =>
      SQL(s"""SELECT count(*) FROM $tableName""")
        .as(scalar[Long].single)
    })
  }

  def cleanupTable(tableName: String)(implicit context: Context): Boolean = {
    import context._
    withDatabase { database =>
      implicit val connection = database.getConnection()
      DB.withConnection({ c =>
        SQL(s"""DELETE FROM $tableName""")
          .execute()
      })
    }
  }

}
