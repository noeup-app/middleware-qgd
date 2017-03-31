package com.noeupapp.middleware.files

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.crudauto.CrudAutoFactory
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.file._
import com.noeupapp.middleware.utils.file.fileUploader.utils.{FileUploadDataParser, FileUploaderFetcher}
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import com.noeupapp.middleware.utils.file.fileUploader.PrepareUploadData._
import com.noeupapp.middleware.utils.file.fileUploader.{PrepareUploadData, PrepareUploadDataFS, PrepareUploadDataS3}
import com.noeupapp.middleware.utils.TypeCustom._
import com.noeupapp.middleware.utils.file.fileUploader.implementations.{FileSystemFileUploader, S3FileUploader}
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import com.noeupapp.middleware.utils.FutureFunctor._

/**
  * Created by damien on 31/03/2017.
  */
class FileService @Inject()(fileService: CrudAutoFactory[File, UUID],
                            fileUploaderFetcher: FileUploaderFetcher,
                            fileUploadDataParser: FileUploadDataParser,
                            fileSystemFileUploader: FileSystemFileUploader) {


  def prepareUpload(target: String, uploadData: JsValue): Future[Expect[File]] = {

    for {
      data <- EitherT(fileUploadDataParser.parseUploadData(target, uploadData))
      fileUploader <- EitherT(fileUploaderFetcher.fetchFileUploader(target))

      fileId = UUID.randomUUID()

      url <- EitherT(fileUploader.prepareFileUrl(fileId, data.asInstanceOf[fileUploader.Data]))
      fileInput <- EitherT(fileUploader.prepareFile(fileId, url, data.asInstanceOf[fileUploader.Data]))

      fileAdded <- EitherT(fileService.add(fileInput))
    } yield fileAdded

  }.run


  def upload(fileId: UUID, inputFile: FilePart[TemporaryFile], user: User): Future[Expect[File]] = {

    val fileName = inputFile.filename

    // Do not handle .tar.gz !
    def getExtension(fileName: String): Option[String] = {
      val lastIndexOfDot = fileName.lastIndexOf(".")
      if (lastIndexOfDot != -1 &&
        lastIndexOfDot != 0) {
        // ex .htaccess
        Some(fileName.substring(lastIndexOfDot + 1))
      } else None
    }

    for {
      fileOpt <- EitherT(fileService.find(fileId))

      file <- EitherT(fileOpt |> "You are trying to upload file not prepared")

      fileToUpdate = file.copy(
        extension = getExtension(fileName),
        sizeBytes = Some(inputFile.ref.file.length),
        mime = inputFile.contentType
      )

      fileUpdated <- EitherT(fileService.update(fileId, fileToUpdate))

      _ <- EitherT(fileSystemFileUploader.upload(file, inputFile))
    } yield fileUpdated
  }.run

}
