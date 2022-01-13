enablePlugins(JavaAppPackaging,DockerPlugin)

ThisBuild / organization := "eu.sia.pagopa"
ThisBuild / scalaVersion := "2.13.6"

libraryDependencies += "com.lihaoyi" %% "cask"   % "0.7.12"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.0.1"

libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.3"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3"
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.3.17"

Docker / packageName := "nodo-dei-pagamenti-mock"
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
dockerExposedPorts := Seq(8087)
dockerUpdateLatest := true

