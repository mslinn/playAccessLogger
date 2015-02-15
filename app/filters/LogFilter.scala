package filters

import java.io.FileWriter
import org.joda.time.format.ISODateTimeFormat
import play.api.mvc.{Filter, RequestHeader, SimpleResult}
import securesocial.core.SecureSocial
import scala.concurrent.Future
import java.io.File
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

/** Adds elapsed time for request (in millis) and free memory (in MB) to combined log format
  * @see http://httpd.apache.org/docs/2.2/logs.html#combined */
object LogFilter extends Filter {
  private lazy val runtime = Runtime.getRuntime
  private lazy val totalMem = runtime.totalMemory
  private lazy val fmt = ISODateTimeFormat.dateTime()

  protected[filters] lazy val fw = openAccessLog

  def apply(nextFilter: RequestHeader => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    implicit val rh = request
    val startTime = System.currentTimeMillis
    nextFilter(request).map { result =>
      val remoteIpAddress = request.remoteAddress
      val userName = SecureSocial.currentUser.map(_.identityId.userId).getOrElse(request.headers.get("REMOTE_USER").getOrElse("-"))

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

  def formatTime(millis: Long): String = fmt.print(millis)

  def log(msg: String) = try {
    fw.write(msg)
    fw.flush()
  } catch {
    case x: Exception => println(x)
  }

  protected[filters] def openAccessLog: FileWriter = {
    val varLogDir = new File("/var/log/cadenza")
    try {
      if (!varLogDir.exists) varLogDir.mkdir()
    } catch {
      case e: Exception => Logger.warn(s"Could not create ${varLogDir.getAbsolutePath}; ${e.getClass.getName}: ${e.getMessage}")
    }
    val logDir = maybeDirectory("/var/log").getOrElse(maybeDirectory(".").getOrElse(new File(System.getProperty("user.home"))))
    val logFile = new File(logDir, "access.log")
    Logger.info(s"Writing to ${logFile.getAbsolutePath}")
    new FileWriter(logFile, true)
  }

  protected[filters] def maybeDirectory(name: String): Option[File] = {
    val file = new File(name)
    if (file.exists && file.isDirectory && file.canWrite) Some(file) else None
  }

  def start() = log(s"\n\nPlay application started ${fmt.print(System.currentTimeMillis)}")

  def stop() = {
    log(s"Play application shut down ${LogFilter.formatTime(System.currentTimeMillis)}")
    fw.close()
  }
}
