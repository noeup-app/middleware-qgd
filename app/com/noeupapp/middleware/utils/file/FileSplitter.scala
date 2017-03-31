package com.noeupapp.middleware.utils.file

import java.io.{FileInputStream, FileOutputStream}

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}

/**
  * Created by damien on 30/03/2017.
  */
class FileSplitter {


  def splitFile(file: java.io.File, fileNames: List[String], directory: String): Future[Expect[Map[String, java.io.File]]] = Future {

    val numberOfSplits = fileNames.length

    if (numberOfSplits <= 0)
      return Future.successful(-\/(FailError("splitFile.numberOfSplits must be > 0")))

    val fis = new FileInputStream(file)

    val files =
      for ((chunkSize, fileName) <- getFilePartSizeBytes(file, numberOfSplits).zip(fileNames))
        yield {
          val buffer = fillBuffer(fis, chunkSize.toInt)
          val filePartCreated = writeFile(buffer, fileName, directory)
          fileName -> filePartCreated
        }

    fis.close()

    \/-(files.toMap)
  }.recover {
    case e: Exception => -\/(FailError("SplitFile error", e))
  }

  private def getFilePartSizeBytes(file: java.io.File, numberOfChunk: Int): List[Long] = {

    val fileLengthBytes: Long = file.length()
    val filePartLengthBytes: Long = fileLengthBytes / numberOfChunk

    for (chunkNumber <- 0 until numberOfChunk)
      yield {
        if (chunkNumber != numberOfChunk - 1) // not last chunk
          filePartLengthBytes
        else
        // if the last chunk size is smaller than others
          fileLengthBytes - (filePartLengthBytes * (numberOfChunk - 1))
      }
  }.toList

  private def fillBuffer(fis: FileInputStream, chunkSize: Int): Array[Byte] = {
    val buffer = new Array[Byte](chunkSize)
    fis.read(buffer, 0, chunkSize)
    buffer
  }

  private def writeFile(bytes: Array[Byte], fileName: String, directory: String): java.io.File = {
    createIntermedateDirectories(directory)
    val file = new java.io.File(directory + "/" + fileName)
    val fos = new FileOutputStream(file)
    fos.write(bytes)
    fos.flush()
    fos.close()
    file
  }


  private def createIntermedateDirectories(directoryPath: String) =
    new java.io.File(directoryPath).mkdirs()

}
