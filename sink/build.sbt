ThisBuild / name := "spark-know"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(name := "sink")

crossScalaVersions := Seq("2.13.8")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

val http4sVersion = "1.0.0-M40"
val kafkaVersion = "3.7.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided"
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-ember-client" % http4sVersion
libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % kafkaVersion

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.4" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test


organization := "com.afjcjsbx.sparknow"
description := "Sparknow is a Scala application that uses Spark listeners to collect metrics from key Spark methods."
developers := List(Developer(
  "afjcjsbx", "afjcjsbx", "afjcjsbx@gmail.com",
  url("https://github.com/afjcjsbx")
))
homepage := Some(url("https://github.com/afjcjsbx/sparknow"))