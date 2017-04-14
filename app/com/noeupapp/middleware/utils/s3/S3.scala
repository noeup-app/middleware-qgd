package com.noeupapp.middleware.utils.s3

import java.io.{ByteArrayInputStream, File, FileOutputStream, InputStream}
import java.util.{Date, UUID}

import com.amazonaws._
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{Headers, AmazonS3Client}
import com.amazonaws.services.s3.model._
import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scalaz.{-\/, \/-}

case class AwsCellarS3(bucketName:String, folder: String, file: Array[Byte], fileName: String)

case class S3Config(host: String, key: String, secret: String, expirationSignedUrlInMinutes: Long, bucket: String)

case class S3CoweboConfig(host: String, key: String, secret: String, expirationSignedUrlInMinutes: Long, bucket: String)

class AmazonS3CoweboClient(awsCredentials: AWSCredentials, clientConfiguration: ClientConfiguration) extends AmazonS3Client(awsCredentials, clientConfiguration) {

}

class S3 @Inject() (s3: AmazonS3Client,
                    s3Config: S3Config,
                    s3CoweboConfig: S3CoweboConfig,
                    s3Cowebo: AmazonS3CoweboClient
                    ) {


  def putStreamObject(content: AwsCellarS3, contentType: String): Future[Expect[String]] = {
    manageS3Error {
      //val tempFile = File.createTempFile(content.fileName, ".tmp")
      //tempFile.deleteOnExit()
      //val fos = new FileOutputStream(tempFile.getPath)
      //fos.write(content.file)
      //fos.close()

      //switch horrible en attendant mieux (vu par Guillaume)
      val stream: InputStream = new ByteArrayInputStream (content.file)

      val meta: ObjectMetadata = new ObjectMetadata ()
      meta.setContentLength (content.file.length.toLong)
//      meta.setContentType (contentType)
      content.bucketName match {
        case "thumb-cowebo-com" =>
          s3Cowebo.putObject(//new PutObjectRequest(
            content.bucketName,
            content.fileName,
            stream,
            meta
          )//.withCannedAcl(CannedAccessControlList.PublicRead)//)
          s3Cowebo.setObjectAcl (content.bucketName, content.fileName, CannedAccessControlList.PublicRead)
          val url = s3Cowebo.getResourceUrl(content.bucketName, content.fileName)
          \/-(url)

        case _ =>
          s3.putObject(//new PutObjectRequest(
            content.bucketName,
            content.fileName,
            stream,
            meta
          )//.withCannedAcl(CannedAccessControlList.PublicRead)//)
          s3.setObjectAcl (content.bucketName, content.fileName, CannedAccessControlList.PublicRead)
          val url = s3.getResourceUrl(content.bucketName, content.fileName)
          \/-(url)
      }
      //tempFile.delete()
    }
//    String bucket = "bucketname.obliquid.com";
//    String fileName = "2011/test/test.pdf";
//    byte[] contents = new byte[] {
//      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
//    };
//    try {
//      AmazonS3 client = new AmazonS3Client (
//        new BasicAWSCredentials ("awsAccessKey", "awsSecretKey") );
//      InputStream stream = new ByteArrayInputStream (contents);
//      ObjectMetadata meta = new ObjectMetadata ();
//      meta.setContentLength (contents.length);
//      meta.setContentType ("application/pdf");
//      client.putObject (bucket, fileName, stream, meta);
//      client.setObjectAcl (bucket, fileName, CannedAccessControlList.PublicRead);
//    } catch (Exception ex) {
//      System.out.println (ex);
//    }
  }


  @Deprecated // TODO : check encoding
  def putObject(content: AwsCellarS3): Future[Expect[String]] = {
    manageS3Error {
      val tempFile = File.createTempFile(content.fileName, ".tmp")
      tempFile.deleteOnExit()
      val fos = new FileOutputStream(tempFile.getPath)
      fos.write(content.file)
      fos.close()

      s3.putObject(new PutObjectRequest(
        content.bucketName,
        content.fileName,
        tempFile
      ).withCannedAcl(CannedAccessControlList.PublicRead))

      val url = s3.getResourceUrl(content.bucketName, content.fileName)

      tempFile.delete()

      \/-(url)
    }
  }

  @Deprecated // TODO : check encoding
  def findById(bucketName: String, fileName: String): Future[Expect[AwsCellarS3]] = {
    manageS3Error {
      val s3obj = s3.getObject(new GetObjectRequest(bucketName, fileName))

      val folderPattern = "\\/[^/]*$".r
      val folder = folderPattern replaceFirstIn(bucketName, "")
      val uuidPattern = "[^/]*$".r
      val uuid = uuidPattern findFirstIn bucketName

      val f = Source.fromInputStream(s3obj.getObjectContent, "ISO-8859-1").map(_.toByte).toArray

      \/-(AwsCellarS3(bucketName, folder, f, uuid.getOrElse("")))
    }
  }

  def getSignedUrlToGetAFile(bucketName: String, fileName: String): Future[Expect[UrlS3]] =
    getSignedUrl(HttpMethod.GET, bucketName, fileName)


//  def getSignedUrlToPutAFile(bucketName: String, fileName: String): Future[Expect[UrlS3]] =
//    getSignedUrlToPutAFile(bucketName, fileName, None)
//
//  def getSignedUrlToPutAFile(bucketName: String, fileName: String, documentInstanceId: Option[UUID]): Future[Expect[UrlS3]] =
//    getSignedUrl(HttpMethod.PUT, bucketName, fileName, documentInstanceId) // TODO set uploadable only once

  def getSignedUrlToPutAFile(bucketName: String, fileName: String, isPublicResource: Boolean): Future[Expect[UrlS3]] =
    getSignedUrlToPutAFile(bucketName, fileName, None, isPublicResource)
  def getSignedUrlToPutAFile(bucketName: String, fileName: String, documentInstanceId: Option[UUID], isPublicResource: Boolean): Future[Expect[UrlS3]] =
    getSignedUrl(HttpMethod.PUT, bucketName, fileName, documentInstanceId, isPublicResource) // TODO set uploadable only once

  def getSignedUrl(httpMethod: HttpMethod, bucketName: String, fileName: String, documentInstanceId: Option[UUID] = None, isPublicResource: Boolean = false): Future[Expect[UrlS3]] = {
    manageS3Error {
      val expiration = new Date()
      val msec = expiration.getTime
      val ratioMiliMinute = 1000 * 60
      val expirationSignedUrlInMili = s3Config.expirationSignedUrlInMinutes * ratioMiliMinute
      expiration.setTime(msec + expirationSignedUrlInMili)

      val generatePreSignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fileName, httpMethod)
      generatePreSignedUrlRequest.setExpiration(expiration)

      if(isPublicResource) {
        // setting http request header:
        // x-amx-canned-acl: 'public-read'
        generatePreSignedUrlRequest.addRequestParameter(
          Headers.S3_CANNED_ACL,
          CannedAccessControlList.PublicRead.toString()
        )
      }

      Logger.debug("key = " + generatePreSignedUrlRequest.getKey())

      val url =
        s3
        .generatePresignedUrl(generatePreSignedUrlRequest)
        .toString
      \/-(UrlS3(url, s3Config.expirationSignedUrlInMinutes, documentInstanceId))
    }
  }

  def getBucketVersioningConfiguration(bucketName: String): Future[Expect[BucketVersioningConfiguration]] = manageS3Error {
    \/-(s3.getBucketVersioningConfiguration(bucketName))
  }

  def setBucketVersioningConfigurationToEnableIfNeeded(bucketName: String): Future[Expect[Unit]] = {
    getBucketVersioningConfiguration(bucketName) flatMap {
      case \/-(conf) =>
        manageS3Error {
          val configuration = new BucketVersioningConfiguration().withStatus("Enabled")

          val setBucketVersioningConfigurationRequest =
            new SetBucketVersioningConfigurationRequest(bucketName ,configuration)
          s3.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest)
          \/-(())
        }
      case -\/(e) =>
        getBucketVersioningConfiguration(bucketName).map(_.map{ conf =>
          Logger.info("S3 bucket versioning configuration statut " + conf.getStatus)
          ()
        })
    }
  }

//  def configureBucket(bucketName: String) = manageS3Error {
//    s3.setRegion(Region.getRegion(Regions.EU_WEST_1))
//
//    // 1. Enable versioning on the bucket.
//    val configuration = new BucketVersioningConfiguration().withStatus("Enabled")
//
//    val setBucketVersioningConfigurationRequest =
//      new SetBucketVersioningConfigurationRequest(bucketName ,configuration)
//
//
//
//    // 2. Get bucket versioning configuration information.
//    val conf = s3.getBucketVersioningConfiguration(bucketName)
//    Logger.info("S3 bucket versioning configuration statut " + conf.getStatus)
//  }


  private def manageS3Error[T](unsafeBlock: Expect[T]): Future[Expect[T]] = {
    try{
      val res = unsafeBlock // unsafeBlock is not called in the future to catch exceptions
      Future.successful(res)
    }
    catch{
      case e:AmazonServiceException => {
        val errorMessage =
          s"""
             |Caught an AmazonServiceException, which means your request made it to Amazon S3, but was rejected with an error response for some reason.
             |     Error Message :    ${e.getMessage}
             |     HTTP Status Code : ${e.getStatusCode}
             |     AWS Error Code :   ${e.getErrorCode}
             |     Error Type :       ${e.getErrorType}
             |     Request ID :       ${e.getRequestId}
              """.stripMargin

        Future.successful(-\/(FailError(s"AmazonServiceException : $errorMessage", e)))
      }
      case e:AmazonClientException => {
        val errorMessage =
          """
            |Caught an AmazonClientException, which means the client encountered a serious internal problem while trying to communicate with S3, such as not being able to access the network.
          """.stripMargin
        Future.successful(-\/(FailError(s"AmazonClientException : $errorMessage", e)))
      }
      case e: Exception =>
        Future.successful(-\/(FailError(s"Exception occurred in S3", e)))
    }
  }

}