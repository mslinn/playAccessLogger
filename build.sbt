name := """play-access-logger"""
organization := "com.micronautics"
version := "1.2.2"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

scalaVersion := "2.11.12"
crossScalaVersions := Seq("2.11.12", "2.12.9")

scalacOptions ++=
  scalaVersion {
    case sv if sv.startsWith("2.10") => List(
      "-target:jvm-1.7"
    )

    case _ => List(
      "-target:jvm-1.8",
      "-Ywarn-unused"
    )
  }.value ++ Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Xlint"
  )

scalacOptions in (Compile, doc) ++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://github.com/mslinn/playAccessLogger/tree/master€{FILE_PATH}.scala"
  )
}.value

resolvers ++= Seq(
  "Lightbend Releases" at "http://repo.typesafe.com/typesafe/releases"
)

libraryDependencies ++= scalaVersion {
  case sv if sv.startsWith("2.12") => // Builds with Scala 2.12.x, Play 2.6.x
    val playV = "2.6.2"
    Seq(
      "com.typesafe.play" %% "play"         % playV    % Provided,
      "com.typesafe.play" %% "play"         % playV    % Test
    )

  case sv if sv.startsWith("2.11") => // Builds with Scala 2.11.x, Play 2.5.x
    val playV = "2.5.16"
    Seq(
      "com.typesafe.play" %% "play"         % playV    % Provided,
      "com.typesafe.play" %% "play"         % playV    % Test
    )

  case sv if sv.startsWith("2.10") => // Builds with Scala 2.10.x, Play 2.2.x
    val playV = "2.2.6"
    Seq(
      "com.typesafe.play" %% "play"         % playV   % Provided,
      "com.typesafe.play" %% "play"         % playV   % Test,
      "ws.securesocial"   %% "securesocial" % "2.1.4" % Test
    )
}.value

bintrayOrganization := Some("micronautics")
bintrayRepository := "play"

publishArtifact in Test := false
