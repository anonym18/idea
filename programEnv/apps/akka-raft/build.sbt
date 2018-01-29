// akka-raft project cloned from commit:
// https://github.com/ktoso/akka-raft/commit/446349939221c69e56cc7b8088bfd23d5f904a6b
// modified to employ DebuggingDispatcher


scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.4") //, "2.10.4")

//unmanagedBase <<= baseDirectory { base => base / "lib" }

connectInput in run := true

//resolvers += Resolver.mavenLocal
//resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

//libraryDependencies ++= Seq(
//  "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.2-SNAPSHOT",


 // "com.debugger" %% "debugging-dispatcher" % "1.0",
 // "com.debugger" %% "protocol" % "1.0"
//)

resolvers += Resolver.mavenLocal
resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
