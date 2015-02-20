package com.micronautics.playFilters

import java.io.{File, FileWriter}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Filter, RequestHeader, SimpleResult}
import scala.concurrent.Future

/** `playAccessLogger` generates access logs for Play 2 applications in an enhanced
  * [[http://httpd.apache.org/docs/2.2/logs.html#combined Apache httpd combined log format]].
  * The following fields are added to each log entry:
  *  - Elapsed time for each request (in milliseconds)
  *  - Free memory when each request completes (in MB)
  *
  * Authenticated user ids are logged if available, and are reported in the third field of each log entry.
  * @param logDirectoryName directory name to write `access.log` to. That directory will be created if required.
  *                         If it cannot be created or cannot be written to or is not specified,
  *                         the Play application's current directory is tried, and if that fails the Play application's `user.home` is tried.
  * @param signOnPrefix defaults to `Play application` but you could provide the name of your application, with version and build number, etc if you like
  * @param lookupUserId is a `Function1` that accepts a `RequestHeader` and returns an `Option[String]` which optionally contains the user id of authenticated users */
class PlayAccessLogger(logDirectoryName: String="", signOnPrefix: String="Play application")(lookupUserId: RequestHeader => Option[String]) extends Filter {
  import PlayAccessLogger._

  protected[playFilters] val maybeFw: Option[FileWriter] = maybeFileWriter

  def apply(nextFilter: RequestHeader => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    implicit val rh = request
    val startTime = System.currentTimeMillis
    nextFilter(request).map { result =>
      val remoteIpAddress: String = {
        // see http://johannburkard.de/blog/programming/java/x-forwarded-for-http-header.html
        val result = request.headers.get("X-Forwarded-For").map(_.split(",").head).getOrElse(
                       request.headers.get("Remote_Addr").getOrElse(
                         request.remoteAddress))
        Logger.debug(s"""request.headers.get("X-Forwarded-For")=${request.headers.get("X-Forwarded-For")}
                        |request.headers.get("Remote_Addr")=${request.headers.get("Remote_Addr")}
                        |request.remoteAddress=${request.remoteAddress}
                        |result=$result""".stripMargin)
        result
      }
      val userName = lookupUserId(rh).getOrElse(request.headers.get("REMOTE_USER").getOrElse("-"))

      val endTime = System.currentTimeMillis
      val elapsedMillis = endTime - startTime
      val timeStamp = fmt.print(startTime)

      val requestSummary = {
        val query = if (request.rawQueryString.nonEmpty) s"?${request.rawQueryString}" else ""
        val protocol = request.headers.get("Content-Length").getOrElse("-")
        val httpVersion = request.version
        s"${request.method} ${request.path}$query $protocol $httpVersion"
      }

      val statusCode = result.header.status
      val contentLength = request.headers.get("Content-Length").getOrElse("-")
      val referrer = request.headers.get("referer").getOrElse("-")
      val userAgent = request.headers.get("User-Agent").getOrElse("-")
      val usedMem = (totalMem - runtime.freeMemory) / 1000000
      val freeMem = runtime.freeMemory / 1000000

      val enhancedCommonLogEntry = s"""$remoteIpAddress - $userName $timeStamp "$requestSummary" $statusCode $contentLength $referrer "$userAgent" $elapsedMillis $usedMem $freeMem\n"""
      Logger.info(enhancedCommonLogEntry)
      log(enhancedCommonLogEntry)

      result
    }
  }

  protected[playFilters] def log(msg: String) = try {
    maybeFw.foreach { fw =>
      fw.write(msg)
      fw.flush()
    }
  } catch {
    case x: Exception => println(x)
  }

  protected[playFilters] def maybeDirectory(name: String): Option[File] = {
    val file = new File(name)
    if (file.exists && file.isDirectory && file.canWrite) {
      Logger.info(s"Writing access logs to ${file.getAbsolutePath}")
      Some(file)
    } else {
      Logger.warn(s"Cannot write access logs to ${file.getAbsolutePath}")
      None
    }
  }

  protected[playFilters] def maybeFileWriter: Option[FileWriter] = {
    val maybeUserLogDir: Option[File] = if (logDirectoryName.trim.isEmpty) None else {
      val logDirFile = new File(logDirectoryName.trim)
      try {
        if (!logDirFile.exists) {
          Logger.debug(s"Attempting to create ${logDirFile.getAbsolutePath}")
          logDirFile.mkdirs()
        }
        if (logDirFile.canWrite) {
          Some(logDirFile)
        } else {
          Logger.warn(s"Cannot write to ${logDirFile.getAbsolutePath}")
          None
        }
      } catch {
        case e: Exception =>
          Logger.warn(s"Could not create ${logDirFile.getAbsolutePath}; ${e.getClass.getName}: ${e.getMessage}")
          None
      }
    }
    val logDir: File = maybeUserLogDir.getOrElse(maybeDirectory(".").getOrElse(new File(System.getProperty("user.home"))))
    val logFile = new File(logDir, "access.log")
    if (logFile.canWrite) {
      Logger.info(s"Writing to ${logFile.getAbsolutePath}")
      Some(new FileWriter(logFile, true))
    } else {
      Logger.info("Cannot write access logs to any directory that was tried.")
      None
    }
  }

  /** Invoke this method from Play's `Global.onStart` method if you would like `PlayAccessLogger` to generate a sign-on
    * message when the Play application starts */
  def start() = log(s"\n\n$signOnPrefix started ${fmt.print(System.currentTimeMillis)}")

  /** Invoke this method from Play's ``Global.onStop` method if you would like `PlayAccessLogger` to generate a sign-off
    * message when the Play application stops under controlled circumstances */
  def stop() = {
    log(s"$signOnPrefix shut down ${fmt.print(System.currentTimeMillis)}")
    maybeFw.foreach(_.close())
  }
}

object PlayAccessLogger {
  val runtime = Runtime.getRuntime
  val totalMem = runtime.totalMemory
  val fmt = ISODateTimeFormat.dateTime()
  val Logger = org.slf4j.LoggerFactory.getLogger("playAccessLogger")
}
