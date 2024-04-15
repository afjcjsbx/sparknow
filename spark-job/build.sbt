ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / name := "spark-job"
ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "spark-job"
  )
crossScalaVersions := Seq("2.13.8")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.5.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.20.0"
