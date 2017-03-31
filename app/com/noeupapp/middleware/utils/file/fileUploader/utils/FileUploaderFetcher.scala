package com.noeupapp.middleware.utils.file.fileUploader.utils

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.file.fileUploader.FileUploader
import com.noeupapp.middleware.utils.file.fileUploader.PrepareUploadData.{FS, S3}
import com.noeupapp.middleware.utils.file.fileUploader.implementations.{FileSystemFileUploader, S3FileUploader}

import scala.concurrent.Future
import scalaz.\/-

/**
  * Created by damien on 31/03/2017.
  */
class FileUploaderFetcher @Inject()(s3FileUploader: S3FileUploader,
                                    fileSystemFileUpload: FileSystemFileUploader) {


  def fetchFileUploader(target: String): Future[Expect[FileUploader]] = target match {
    case S3 => Future.successful(\/-(s3FileUploader))
    case FS => Future.successful(\/-(fileSystemFileUpload))
  }

}
