name := """debugging-dispatcher"""

resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

//unmanagedBase <<= baseDirectory { base => base / "../protocol/target/scala-2.11" }

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.2-SNAPSHOT"
)
