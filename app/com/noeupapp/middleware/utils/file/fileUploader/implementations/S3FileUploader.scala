package com.noeupapp.middleware.utils.file.fileUploader.implementations

import java.io.FileOutputStream
import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.files.{File, FileIn}
import com.noeupapp.middleware.utils.file.fileUploader.{FileUploader, PrepareUploadData, PrepareUploadDataS3}
import com.noeupapp.middleware.utils.s3.S3
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Future
import scalaz.{\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 31/03/2017.
  */
class S3FileUploader @Inject()(s3: S3) extends FileUploader {


  override type Data = PrepareUploadDataS3

  override def prepareFileUrl(fileId: UUID, prepareUploadData: Data): Future[Expect[String]] =
    s3.getSignedUrlToPutAFile(
      prepareUploadData.directory,
      prepareUploadData.filename,
      true // isPublic TODO pass it as a parameter
    ).map(_.map(_.url))

  override def prepareFile(fileId: UUID, url: String, prepareUploadData: Data): Future[Expect[File]] = {
    val fileIn = FileIn(
      url = url,
      extension = Some(prepareUploadData.extension),
      name = prepareUploadData.filename,
      sizeBytes = Some(prepareUploadData.sizeBytes),
      mime = Some(prepareUploadData.mime)
    )
    Future.successful(\/-(File.fromIn(fileIn).copy(id = fileId)))
  }


}
