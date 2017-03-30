package com.noeupapp.middleware.files


import java.util.UUID

import com.noeupapp.middleware.crudauto.{Entity, PKTable}
import com.noeupapp.middleware.utils.GlobalReadsWrites
import org.joda.time.DateTime
import play.api.libs.json.Json
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._


import scala.language.{implicitConversions, postfixOps}


case class File(id: UUID,
                url: String,
                updated: DateTime,
                created: DateTime,
                extension: Option[String],
                name: String,
                sizeBytes: Option[Int],
                mime: Option[String]) extends Entity[UUID] {

  def this(e: FileIn) = this(UUID.randomUUID(), e.url, e.updated, e.created, e.extension, e.name, e.sizeBytes, e.mime)

  override def withNewId(id: UUID): Entity[UUID] = copy(id = id)

}


case class FileIn(url: String,
                  updated: DateTime,
                  created: DateTime,
                  extension: Option[String],
                  name: String,
                  sizeBytes: Option[Int],
                  mime: Option[String])

case class FileOut(id: UUID,
                   url: String,
                   updated: DateTime,
                   created: DateTime,
                   extension: Option[String],
                   name: String,
                   sizeBytes: Option[Int],
                   mime: Option[String])

object File extends GlobalReadsWrites {

  implicit val FileFormat = Json.format[File]
  implicit val FileInFormat = Json.format[FileIn]
  implicit val FileOutFormat = Json.format[FileOut]

  val files = TableQuery[FileTableDef]

  implicit def toFileOut(e: File): FileOut = FileOut(e.id, e.url, e.updated, e.created, e.extension, e.name, e.sizeBytes, e.mime)

}


class FileTableDef(tag: Tag) extends Table[File](tag, "files") with PKTable {

  def id = column[UUID]("id")
  def url = column[String]("url")
  def updated = column[DateTime]("updated")
  def created = column[DateTime]("created")
  def extension = column[Option[String]]("extension")
  def name = column[String]("name")
  def sizeBytes = column[Option[Int]]("size_bytes")
  def mime = column[Option[String]]("mime")

  override def * = (id, url, updated, created, extension, name, sizeBytes, mime) <> ((File.apply _).tupled, File.unapply)

  def pk = primaryKey("files_pk", id)


}


