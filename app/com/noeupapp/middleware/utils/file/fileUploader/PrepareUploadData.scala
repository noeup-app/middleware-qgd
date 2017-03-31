package com.noeupapp.middleware.utils.file.fileUploader

import play.api.libs.json.Json

/**
  * Created by damien on 31/03/2017.
  */
trait PrepareUploadData {

  // Directory or bucket
  val directory: String

  val filename: String

}

object PrepareUploadData {
  val S3 = "s3"
  val FS = "fs"
}


case class PrepareUploadDataS3(directory: String,
                               filename: String,
                               extension: String,
                               sizeBytes: Long,
                               mime: String) extends PrepareUploadData

object PrepareUploadDataS3 {
  implicit val prepareUploadDataS3Format = Json.format[PrepareUploadDataS3]
}


case class PrepareUploadDataFS(directory: String,
                               filename: String) extends PrepareUploadData


object PrepareUploadDataFS {
  implicit val prepareUploadDataFSFormat = Json.format[PrepareUploadDataFS]
}