import sbt._
import sbt.Keys._
import com.github.retronym.SbtOneJar

object ScalaTimeMachineBuild extends Build {
  def standardSettings = Seq(
    exportJars := true
  ) ++ Defaults.defaultSettings
  
  lazy val scalaTimeMachine = Project(
    id = "scala-time-machine",
    base = file("."),
    settings = standardSettings ++ SbtOneJar.oneJarSettings ++ Seq(
      name := "Scala time machine",
      organization := "net.defoo",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.2",
      libraryDependencies ++= Seq(
        "commons-io" % "commons-io" % "2.4",
        "com.typesafe.akka" %% "akka-actor" % "2.2.3",
        "com.typesafe.akka" %% "akka-slf4j" % "2.2.3",
        "ch.qos.logback" % "logback-classic" % "1.0.7",
        "com.github.scopt" %% "scopt" % "3.2.0")
      // add other settings here
    )
  )
}
