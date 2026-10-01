name := """scala-api-store"""
organization := "com.mena"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

// Scala 2.13 with Scala 3 syntax rules where the compiler supports them
scalacOptions ++= Seq(
  "-Xsource:3",
  "-feature",
  "-deprecation",
  "-Wunused"
)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  "com.lucidchart" %% "relate" % "5.1.0",
  "com.mysql" % "mysql-connector-j" % "9.7.0",
  "org.flywaydb" % "flyway-core" % "13.8.1",
  "org.flywaydb" % "flyway-mysql" % "13.8.1",
  "com.github.pureconfig" %% "pureconfig" % "0.17.10",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.44.1" % Test,
  "com.dimafeng" %% "testcontainers-scala-mysql" % "0.44.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
)