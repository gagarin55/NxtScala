organization := "nxt"

name := "nxtscala"

version := "0.3.3"

scalaVersion := "2.11.8"

scalacOptions in Test ++= Seq("-Yrangepos")

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases/"

resolvers += "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= Seq(
    "org.scala-lang.plugins" %% "scala-continuations-library" % "1.0.2",
    "com.github.nscala-time" %% "nscala-time" % "1.6.0",
    "com.jsuereth" % "scala-arm_2.10" % "1.3",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

publishMavenStyle := true

publishTo := Some(Resolver.file("nxtscala", new File( "../repo" )))