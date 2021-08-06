
name := "ImgurImageUploader"

version := "0.1"

scalaVersion := "2.12.8"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.5.0",
  "org.typelevel" %% "cats-effect" % "1.1.0",
  "io.chrisdavenport" %% "cats-par" % "0.2.0",

  "co.fs2" %% "fs2-core" % "1.0.0",
  "co.fs2" %% "fs2-io" % "1.0.0",
  "eu.timepit" %% "fs2-cron-core" % "0.0.10",

  "org.http4s" %% "http4s-dsl" % "0.19.0",
  "org.http4s" %% "http4s-blaze-server" % "0.19.0",
  "org.http4s" %% "http4s-blaze-client" % "0.19.0",
  "org.http4s" %% "http4s-circe" % "0.19.0",
  "org.http4s" %% "http4s-dsl" % "0.19.0",

  "io.circe" %% "circe-core" % "0.10.1",
  "io.circe" %% "circe-generic" % "0.10.1",

  "io.chrisdavenport" %% "linebacker" % "0.2.0",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.2.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.github.pureconfig" %% "pureconfig" % "0.10.1",
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.10.1",

  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "io.chrisdavenport" %% "log4cats-noop" % "0.2.0" % "test"
)

addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.9")
addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4")


wartremoverErrors ++= Warts.unsafe
wartremoverErrors -= Wart.Any // false warnings

//daemonUser.in(Docker) := "root"
mainClass in Compile := Some("com.leadiq.main.Main")
maintainer.in(Docker) := "san"
version.in(Docker) := "latest"
dockerExposedPorts := Vector(8080)
dockerRepository := Some("sanantoha")