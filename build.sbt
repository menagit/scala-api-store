name := """scala-api-store"""
organization := "com.mena"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala,SwaggerPlugin)

scalaVersion := "2.13.18"

// Scala 2.13 with Scala 3 syntax rules where the compiler supports them
scalacOptions ++= Seq(
  "-Xsource:3",
  "-Xsource-features:case-apply-copy-access",
  "-feature",
  "-deprecation",
  "-Wunused"
)

Test / parallelExecution := false

swaggerDomainNameSpaces := Seq("com.mendev.apistore")
swaggerV3 := true

libraryDependencies ++= Seq(
  jdbc,
  "com.lucidchart" %% "relate" % "5.1.0",
  "com.mysql" % "mysql-connector-j" % "9.7.0",
  "org.flywaydb" % "flyway-core" % "13.8.1",
  "org.flywaydb" % "flyway-mysql" % "13.8.1",
  "com.github.pureconfig" %% "pureconfig" % "0.17.10",
  "com.password4j" % "password4j" % "1.8.4",
  "com.github.jwt-scala" %% "jwt-play-json" % "11.0.4",
  "com.bucket4j" % "bucket4j_jdk17-core" % "8.18.0",
  "org.playframework" %% "play-mailer" % "10.1.0",
  "org.sangria-graphql" %% "sangria"                  % "4.2.19",
  "org.sangria-graphql" %% "sangria-play-json-play30" % "2.0.3",
  "org.webjars" % "swagger-ui" % "5.10.3",
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.44.1" % Test,
  "com.dimafeng" %% "testcontainers-scala-mysql" % "0.44.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
)

coverageExcludedPackages := "<empty>;router\\..*;controllers\\.ReverseHealthController;controllers\\.javascript\\..*"