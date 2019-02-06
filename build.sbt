val finchVersion = "0.26.0"
val circeVersion = "0.10.1"
val scalatestVersion = "3.0.5"

lazy val root = (project in file("."))
  .settings(
    organization := "com.nd",
    name := "validation-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core"  % finchVersion,
      "com.github.finagle" %% "finchx-circe"  % finchVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test",
      "org.scalaj" %% "scalaj-http" % "2.4.1",
      "com.typesafe" % "config" % "1.3.0"
    )
  )