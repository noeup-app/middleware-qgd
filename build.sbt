name := "oauth2ProvideAuthenticateAndAuthorize"

version := "1.0"

lazy val `oauth2provideauthenticateandauthorize` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "0.16.1",
  "com.mohiva" % "play-silhouette_2.11" % "3.0.4",
  "com.typesafe.play" %% "play-slick" % "1.1.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.1.1",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "com.typesafe.play" %% "play-mailer" % "3.0.1"
)

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )
