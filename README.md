Secure Access Logger
====================

To activate the access logger, include `LogFilter` into your Play application's list of filters.

This filter logs the SecureSocial user name in the third field if the user is logged in. The first log entry below shows unauthenticated access; the second log entry shows access by user mslinn.
````
127.0.0.1 - - 2015-02-14T15:56:39.675-08:00 "POST /authenticate/userpass 33 HTTP/1.1" 303 33 http://localhost:9000/login "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/40.0.2214.111 Chrome/40.0.2214.111 Safari/537.36" 988 126 1555
127.0.0.1 - mslinn 2015-02-14T15:56:40.668-08:00 "GET /courses/admin/show/40 - HTTP/1.1" 200 - http://localhost:9000/login "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/40.0.2214.111 Chrome/40.0.2214.111 Safari/537.36" 12750 425 1256
````

Here is a sample `app/Global.scala` showing how to set up the logger.
````
import filters._
import play.api.{Application, GlobalSettings }
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.filters.gzip.GzipFilter

object Global extends WithFilters(LogFilter, new GzipFilter) with GlobalSettings {
  /** If you want LogFilter to sign on with a message like the following, call `LogFilter.start()` from `Global.onStart`:
      <pre>Play application started 2015-02-14T15:56:18.566-08:00</pre> */
  override def onStart(app: Application) = LogFilter.start()

  /** If you want LogFilter to sign off with a message like the following, call `LogFilter.stop()` from `Global.onStop`:
      <pre>Play application shut down 2015-02-14T16:12:11.423-08:00</pre> */
  override def onStop(app: Application): Unit = {
    try {
      LogFilter.stop()
    } catch {
      case e: Exception =>
    }
  }
}
````
