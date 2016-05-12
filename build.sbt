name := "middleware-noeupapp"

version := "1.0"

lazy val `middleware-noeupapp` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  // SECURITY
  filters,
  //
  // DATABASE
  "com.typesafe.play" %% "anorm" % "2.4.0",
  evolutions,
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  //"com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  "com.typesafe.play.modules" %% "play-modules-redis" % "2.4.0",
  "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
  jdbc,
  //
  // S3
  "com.amazonaws" % "aws-java-sdk" % "1.10.72",
  //
  // TESTS
  specs2 % Test,
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test",
  //
  // AUTHENTICATION
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.16.1",
  "com.mohiva" % "play-silhouette_2.11" % "3.0.4",
  //"com.mohiva" %% "play-silhouette" % "3.0.0",
  //
  // TOOLS
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  ws,
  cache,
  //
  // FRONT
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",
  "org.webjars" %% "webjars-play" % "2.4.0-1"
)

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "bintray Repository" at "https://dl.bintray.com/graingert/maven"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator



scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)


// The following code is used to set a configuration (env. vars.) for a task
//
//   e.g. $ sbt dev run
//     This command injects environment variables defined in `conf/env/database.env`
//
// To add another configuration :
//   - add a taskKey
//   - add a conf file in conf/env/
//   - define the task by calling setEnvVar() function

fork := true

val dev = taskKey[Unit]("Dev config")
val local = taskKey[Unit]("Local config")

def setEnvVar(env: String) = {
  try{
    val split: Array[String] = (s"cat conf/env/database.$env" !!).split("\\n")
    val raw_vars = split.map(_.span(! _.equals('='))).map(x => x._1 -> x._2.tail).toList
    val sysProp = System.getProperties
    raw_vars foreach (v => {
      println(s"INJECTING ${v._1} = ${v._2}")
      sysProp.put(v._1, v._2)
    })
    System.setProperties(sysProp)
  }catch{
    case e: Exception => println(s"Cannot inject env vars (${e.getMessage})")
  }
}

dev := {
  setEnvVar("dev")
}

local := {
  setEnvVar("dev.default")
}
