name := "debugging-dispatcher"

lazy val commonSettings = Seq(
  organization := "com.debugger",
  version := "1.0",
  // publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))
  scalaVersion := "2.12.1"   // 2.11.0 for projects with Scala 11
)

lazy val protocol = (project in file("protocol"))
  .settings(
    commonSettings
    // other settings
  )


lazy val dispatcher = (project in file("dispatcher"))
  .dependsOn(protocol)
  .settings(
    commonSettings,
    connectInput in run := true
    // other settings
  )

lazy val server = (project in file("server"))
  .dependsOn(protocol)
  .settings(
    commonSettings
    // other settings
  )

