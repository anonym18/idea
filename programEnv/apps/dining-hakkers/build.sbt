name := """dining-hakkers"""

version := "1.0"

scalaVersion := "2.12.1"

resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.2-SNAPSHOT",

  // groupID % artifactID % revision

  "com.debugger" %% "debugging-dispatcher" % "1.0",
  "com.debugger" %% "protocol" % "1.0"
)

