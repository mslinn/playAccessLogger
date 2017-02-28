package com.micronautics.playFilters

import play.api.mvc.RequestHeader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.ExecutionContext

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
class PlayAccessLogger(
  logDirectoryName: String="",
  signOnPrefix: String="Play application"
)(
  val lookupUserId: RequestHeader => Option[String]
) extends PlayAccessLoggerLike {
  implicit val ec: ExecutionContext = defaultContext
}
