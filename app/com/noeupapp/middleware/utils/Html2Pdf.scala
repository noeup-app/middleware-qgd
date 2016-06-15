package com.noeupapp.middleware.utils


import java.io.ByteArrayOutputStream
import javax.inject.Inject

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger
import play.api.libs.ws._
import play.api.libs.iteratee._

import scala.concurrent.Future
import scalaz._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class Html2PdfConfig(url: String, timeout: Long)

class Html2Pdf @Inject()(ws: WSClient,
                         html2PdfConfig: Html2PdfConfig){

  def html2PdfWS(html: String,
                 sens: Option[String] = None,
                 format: Option[String] = None,
                 langue: Option[String] = None,
                 marges: Option[String] = None,
                 header: Option[String] = None,
                 footer: Option[String] = None,
                 backtop: Option[String] = None,
                 backbottom: Option[String] = None): Future[Expect[Array[Byte]]] = {

    val requestPdf: WSRequest = ws.url(html2PdfConfig.url)
    val complexRequest: WSRequest =
      requestPdf
        .withMethod("POST")
        .withBody(Map("body" -> Seq(html),
          "sens" -> Seq(sens.getOrElse("P")),
          "format" -> Seq(format.getOrElse("A4")),
          "langue" -> Seq(langue.getOrElse("fr")),
          "marges" -> Seq(marges.getOrElse("[10, 10, 10, 10]")),
          "header" -> Seq(header.getOrElse("<page_header> <h1>No page header</h1> </page_header>")),
          "footer" -> Seq(footer.getOrElse("<page_footer> <h1>This is a page footer</h1> </page_footer>")),
          "backtop" -> Seq(backtop.getOrElse("15%")),
          "backbottom" -> Seq(backbottom.getOrElse("15%"))
        ))
        .withRequestTimeout(html2PdfConfig.timeout)

    val futureResponse: Future[(WSResponseHeaders, Enumerator[Array[Byte]])] =
      complexRequest.stream()

    val status = futureResponse map {
      case (a,_) => a.status
    }
    status.map(s => Logger.debug(s"Html2Pdf ws response code : ${s.toString}"))
    status flatMap {
      case it if it >= 200 && it <= 299 =>
        val resultFile = futureResponse.flatMap {
          case (headers, body) =>
            val out = new ByteArrayOutputStream()

            // The iteratee that writes to the output stream
            val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
              out.write(bytes)
            }
            // Feed the body into the iteratee
            (body |>>> iteratee).andThen {
              case result =>
                // Close the output stream whether there was an error or not
                out.close()
                // Get the result or rethrow the error
                result.get
            }.map(_ => \/-(out.toByteArray))
        }
        resultFile

      case _ => Future.successful(-\/(FailError("Couldn't retrieve Pdf file from Html2Pdf")))
    }
  }

}
