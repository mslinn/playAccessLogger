name := """play-access-logger"""
organization := "com.micronautics"
version := "1.1.3"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.10.6"
crossScalaVersions := Seq("2.10.6", "2.11.7")

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
    "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in (Compile, doc) <++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://github.com/mslinn/playAccessLogger/tree/master€{FILE_PATH}.scala"
  )
}

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
)

libraryDependencies <++= scalaVersion {
  case sv if sv.startsWith("2.11") => // Builds with Scala 2.11.x, Play 2.3.x
    val playV = "2.3.8"
    Seq(
      "com.typesafe.play" %% "play"         % playV    % "provided",
      "com.typesafe.play" %% "play"         % playV    % "test",
      "ws.securesocial"   %% "securesocial" % "3.0-M3" % "test"
    )

  case sv if sv.startsWith("2.10") => // Builds with Scala 2.10.x, Play 2.2.x
    val playV = "2.2.6"
    Seq(
      "com.typesafe.play" %% "play"         % playV   % "provided",
      "com.typesafe.play" %% "play"         % playV   % "test",
      "ws.securesocial"   %% "securesocial" % "2.1.4" % "test"
    )
}

bintrayOrganization := Some("micronautics")
bintrayRepository := "play"

publishArtifact in Test := false
