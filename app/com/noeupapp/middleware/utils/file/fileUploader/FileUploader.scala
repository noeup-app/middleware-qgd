package com.noeupapp.middleware.utils.file.fileUploader

import java.util.UUID

import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.files.{File, FileIn}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Future

/**
  * Created by damien on 31/03/2017.
  */
trait FileUploader {

  type Data <: PrepareUploadData

  def prepareFileUrl(fileId: UUID, prepareUploadData: Data): Future[Expect[String]]

  def prepareFile(fileId: UUID, url: String, prepareUploadData: Data): Future[Expect[File]]

}
