import Dependencies._

ThisBuild / scalaVersion     := "2.12.7"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.dmitescu"
ThisBuild / organizationName := "dmitescu"

lazy val root = (project in file("."))
  .settings(
    name := "app",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "1.3.0" withSources() withJavadoc(),
      "org.typelevel" %% "cats-core" % "2.0.0-M1" withSources() withJavadoc(),
      "org.typelevel" %% "cats-free" % "2.0.0-M1" withSources() withJavadoc(),

      "com.github.finagle" %% "finch-core" % "0.29.0",
      "com.github.finagle" %% "finch-generic" % "0.29.0",
      "com.github.finagle" %% "finch-circe" % "0.29.0",

      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",

      "org.log4s" %% "log4s" % "1.7.0",
      "org.slf4j" % "slf4j-simple" % "1.7.26",

      "org.specs2" %% "specs2-core" % "4.3.4" % "test"
    ),

    assemblyOutputPath in assembly := baseDirectory.value / "app.jar",
    assemblyMergeStrategy in assembly := {
      case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
