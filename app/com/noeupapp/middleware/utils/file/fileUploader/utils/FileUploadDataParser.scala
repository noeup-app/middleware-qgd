package com.noeupapp.middleware.utils.file.fileUploader.utils

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.file.fileUploader.PrepareUploadData.{FS, S3}
import com.noeupapp.middleware.utils.file.fileUploader.{PrepareUploadData, PrepareUploadDataFS, PrepareUploadDataS3}
import play.api.libs.json.{Format, JsError, JsSuccess, JsValue}

import scala.concurrent.Future
import scalaz.{-\/, \/-}

/**
  * Created by damien on 31/03/2017.
  */
class FileUploadDataParser @Inject()() {

  def parseUploadData(target: String, uploadData: JsValue): Future[Expect[PrepareUploadData]] = {

    def validateOfFail[T: Format](uploadData: JsValue) = uploadData.validate[T] match {
      case JsSuccess(value, _) => Future.successful(\/-(value))
      case JsError(errors) => Future.successful(-\/(FailError(s"parseUploadData error $errors")))
    }

    target.toLowerCase match {
      case S3 => validateOfFail[PrepareUploadDataS3](uploadData)
      case FS => validateOfFail[PrepareUploadDataFS](uploadData)
    }
  }

}
