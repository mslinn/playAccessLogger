# Play 2 Access Logger

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[ ![Download](https://api.bintray.com/packages/micronautics/play/play-access-logger/images/download.svg) ](https://bintray.com/micronautics/play/play-access-logger/_latestVersion)

![play-access-logger logo](http://d357e4bjq673rk.cloudfront.net/1/html/ScalaCore/assets/images/play-access-log.png "play-access-logger Logo")

Play Framework does not provide a facility for generating access logs, so you cannot monitor user activity.
If Play is front-ended with nginx or Apache httpd, the generated logs by the front end process does not contain user ids,
which makes it difficult to track user activity. `play-access-logger` solves these problems.

This project is sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/),
the company that delivers online Scala and Play training via [ScalaCourses.com](http://www.ScalaCourses.com).
You can learn exactly how this filter works by taking the [Introduction to Scala](http://www.ScalaCourses.com/showCourse/40),
[Intermediate Scala](http://www.ScalaCourses.com/showCourse/45) and [Introduction to Play](http://www.ScalaCourses.com/showCourse/39) courses.

`play-access-logger` generates access logs for Play 2 applications in an enhanced
[Apache httpd combined log format](http://httpd.apache.org/docs/2.2/logs.html#combined).
The following fields are added to each log entry:
 1. Elapsed time for each request (in milliseconds)
 1. Free memory when each request completes (in MB)

Proxy-aware IP detection was tested against Heroku, so Heroku apps can report the originating IP address properly.

Authenticated user ids are logged if available, and are reported in the third field of each log entry.
Here is a sample log generated by `playAccessLogger` embedded in the Cadenza Play application that powers
[ScalaCourses.com](http://ScalaCourses.com);
the first log entry shows unauthenticated access; the second log entry shows access by user `mslinn`.

````
Cadenza v0.1.5 build 1381 started 2015-02-14T13:32:25.825-08:00
127.0.0.1 - - 2015-02-14T15:56:39.675-08:00 "POST /authenticate/userpass 33 HTTP/1.1" 303 33 http://localhost:9000/login "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/40.0.2214.111 Chrome/40.0.2214.111 Safari/537.36" 988 126 1555
127.0.0.1 - mslinn 2015-02-14T15:56:40.668-08:00 "GET /courses/admin/show/40 - HTTP/1.1" 200 - http://localhost:9000/login "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/40.0.2214.111 Chrome/40.0.2214.111 Safari/537.36" 12750 425 1256
````

## Installation
This project is built against the following combinations of Scala and Play:
 - Scala 2.12 / Play 2.6.0-RC1
 - Scala 2.11 / Play 2.5.14
 - Scala 2.10 / Play 2.2.6 (v1.1.3, no further updates)

The correct version of the project is selected based on the Scala compiler version.
Other combinations of Scala and Play are not supported.
Versions of Play 2 older than Play 2.2.6 may work but have not been tested.

Add `play-access-logger` to your project's `build.sbt`:

    resolvers += "micronautics/play on bintray" at "http://dl.bintray.com/micronautics/play"

    libraryDependencies +=  "com.micronautics" %% "play-access-logger" % "1.2.2" withSources()

## Usage
Include a `PlayAccessLogger` instance into your Play application's list of filters.
The constructor can optionally accept the directory name to write `access.log` to.
That directory will be created if required.
If it cannot be created or cannot be written to or is not specified, the Play application's current directory is tried next,
and if that fails the Play application's `user.home` is tried before giving up on writing the to a file.

In truth, `play-access-logger` does not completely give up - it also logs `INFO` messages to a `slf4j` `Logger` called `playAccessLogger` for every hit.
Additional output is available if `playAccessLogger` is set to `DEBUG`. You can suppress these potentially redudant messages by setting `playAccessLogger` to `WARN`.

If you want `play-access-logger` to sign on with a message like the following, call `playAccessLogger.start()` from `Global.onStart`:

    Play application started 2015-02-14T15:56:18.566-08:00

If you want `play-access-logger` to sign off with a message like the following, call `playAccessLogger.stop()` from `Global.onStop`:

    Play application shut down 2015-02-14T16:12:11.423-08:00

### Minimal Example
Here is a minimal example for Play 2.3.x:

````
import com.micronautics.playFilters.PlayAccessLogger
import play.api.mvc._

object Global extends WithFilters(new PlayAccessLogger())
````

### Example of Co-operation with Another Filter
You can use any userId mechanism you like in order to include user ids in the access log.
To do this, provide a `Function1` that accepts a `RequestHeader` and returns an `Option[String]` to the second parameter
list of the `PlayAccessLogger` constructor.

Here is a sample `app/Global.scala` showing how to set up the logger, using the obsolete [SecureSocial](http://securesocial.ws/)
package for authentication.
`SecureSocial` must be integrated and configured as usual; see that projects' documentation for further information.
`playAccessLogger` can be chained with other Play filters as per the
[Play documentation](https://www.playframework.com/documentation/2.5.x/ScalaHttpFilters);
this example shows `GzipFilter` being used to compress each response.

````
import com.micronautics.playFilters.PlayAccessLogger
import play.api.{ Application, GlobalSettings }
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import play.filters.gzip.GzipFilter
import securesocial.core.SecureSocial

object X {
  val prefix = "Cadenza v0.1.5"
  val ssUserId: RequestHeader => Option[String] =
    SecureSocial.currentUser(_: RequestHeader).map(_.identityId.userId)
  val playAccessLogger = new PlayAccessLogger("/var/log/cadenza/access.log", prefix)(ssUserId)
}

object Global extends WithFilters(X.playAccessLogger, new GzipFilter) with GlobalSettings {
  override def onStart(app: Application) = X.playAccessLogger.start()

  override def onStop(app: Application): Unit = {
    try {
      X.playAccessLogger.stop()
    } catch {
      case e: Exception => Logger.warn(s"${e.getName.getClass} ${e.getMessage}")
    }
  }
}
````

## Revision History

Version  | Change
-------- | ------
1.2.2    | Scala 2.11 version is now built against Play 2.5.x instead of 2.3.x, removed most references to SecureSocial
1.1.3    | Now writes signon message to Logger, not `access.log`
1.1.2    | Updated dependencies, now creates `access.log` if necessary
1.1.1    | Added proxy support, successfully tested on Heroku
1.1.0    | Removed dependency on [SecureSocial](http://securesocial.ws/), now supports any authentication mechanism; documented how to integrate with `SecureSocial`
1.0.0    | Hived from [Cadenza](http://www.micronauticsresearch.com/products/cadenza/index.html), which powers [ScalaCourses.com](http://ScalaCourses.com)
