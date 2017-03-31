package com.noeupapp.middleware.utils.file.fileUploader.implementations

import java.io.{FileInputStream, FileOutputStream}
import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.config.AppConfig
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.files.{File, FileIn}
import com.noeupapp.middleware.utils.file.fileUploader.{FileUploader, PrepareUploadData, PrepareUploadDataFS}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.mvc.MultipartFormData.FilePart
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{\/, \/-}

/**
  * Created by damien on 31/03/2017.
  */
class FileSystemFileUploader @Inject()(appConfig: AppConfig) extends FileUploader {

  val LOCAL_PATH_PREFIX = "./public/files"
  val REMOTE_PATH_PREFIX = "/assets/files"

  override type Data = PrepareUploadDataFS

  override def prepareFileUrl(fileId: UUID, prepareUploadData: Data): Future[Expect[String]] =
    Future.successful(\/-(appConfig.appUrl + REMOTE_PATH_PREFIX + prepareUploadData.directory + "/" + fileId))

  override def prepareFile(fileId: UUID, url: String, prepareUploadData: Data): Future[Expect[File]] = {
    val fileIn = FileIn(
      url = url,
      extension = None,
      name = prepareUploadData.filename,
      sizeBytes = None,
      mime = None
    )

    Future.successful(\/-(
      File.fromIn(fileIn).copy(id = fileId)
    ))
  }


  def upload(file: File, inputFile: FilePart[TemporaryFile]): Future[Expect[Unit]] = Future {
    val fileDirectory = LOCAL_PATH_PREFIX + file.directory.drop(appConfig.appUrl.length)

    createIntermedateDirectories(fileDirectory)

    val localFile = new java.io.File(LOCAL_PATH_PREFIX + file.url.drop(appConfig.appUrl.length))

    println(localFile)

    val fos = new FileOutputStream(localFile)
    val fis = new FileInputStream(inputFile.ref.file)

    fos getChannel() transferFrom(
      fis.getChannel, 0, Long.MaxValue)

    fos.close()
    fis.close()
    \/-(())
  }



  private def createIntermedateDirectories(directoryPath: String) =
    new java.io.File(directoryPath).mkdirs()

}
