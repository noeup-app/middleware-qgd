name := "oauth2ProvideAuthenticateAndAuthorize"

version := "1.0"

lazy val `oauth2provideauthenticateandauthorize` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  //jdbc,
  cache,
  ws,
  filters,
  specs2 % Test,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.16.1",
  "com.mohiva" % "play-silhouette_2.11" % "3.0.4",
  //"com.mohiva" %% "play-silhouette" % "3.0.0",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "com.mohiva" %% "play-silhouette-testkit" % "3.0.0" % "test",
  "com.typesafe.play" %% "play-mailer" % "3.0.1"
)

resolvers := ("Atlassian Releases" at "https://maven.atlassian.com/public/") +: resolvers.value

resolvers += Resolver.sonatypeRepo("snapshots")

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases"
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