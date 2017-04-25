package com.noeupapp.middleware.files

import java.util.UUID

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.utils.file.fileUploader.FileUploader
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 31/03/2017.
  */
class Files @Inject()(val messagesApi: MessagesApi,
                      val env: Environment[Account, CookieBearerTokenAuthenticator],
                      fileService: FileService)
  extends Silhouette[Account, CookieBearerTokenAuthenticator] {


  def prepareUpload() = SecuredAction.async(parse.json) { request =>

    val uploadData = request.body

    val allowedMethods = List("s3", "fs")

    (uploadData \ "target").validate[String] match {
      case JsSuccess(target, _) if allowedMethods.contains(target.toLowerCase) =>
        fileService.prepareUpload(target, uploadData).map {
          case \/-(file) => Ok(Json.toJson(file))
          case -\/(error) =>
            Logger.error(error.toString)
            InternalServerError("Internal server error")
        }
      case JsSuccess(_, _) =>
        Future.successful(
          BadRequest(s"Target field has to be either ${allowedMethods.map(e => s"`$e`").mkString(" or ")} (case not sensitive)"))
      case JsError(errors) =>
        Logger.error(s"Field `target` is missing : $errors")
        Future.successful(BadRequest("Field `target` is missing"))
    }


  }


  def upload(id: UUID) = SecuredAction.async(parse.multipartFormData) { request =>

    request.body.file("file").map { inputFile =>

      val user = request.identity.user

      fileService.upload(id, inputFile, user).map {
        case \/-(file) => Ok(Json.toJson(file))
        case -\/(error) =>
          Logger.error(error.toString)
          InternalServerError("Internal server error")
      }
    }.getOrElse {
      Future.successful(BadRequest("File is missing"))
    }

  }

  def directDownload(file: String) = SecuredAction { request =>

      val user = request.identity.user
      val url = "/home/git/uploads"


      Ok.sendFile(new java.io.File(url + "/" + file), inline=true)
        .withHeaders( CACHE_CONTROL->"max-age=3600",
                      CONTENT_DISPOSITION->"attachment; filename=download.file",
                      CONTENT_TYPE->"application/x-download");
    // TODO
    //.getOrElse {
    //  Future.successful(BadRequest("File is missing"))
    //}
  }

}
