ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / name := "spark-know"
ThisBuild / scalaVersion := "2.13.8"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided",
)

lazy val root = (project in file("sparknow"))
  .settings(
    name := "sparknow",
    version := "1.0")

lazy val sink = (project in file("sink"))
  .settings(name := "sink", version := "1.0")
  .dependsOn(root)

lazy val sparkjob = (project in file("spark-job"))
  .settings(name := "spark-job", version := "1.0")
  .dependsOn(root)

