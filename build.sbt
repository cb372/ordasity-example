scalaVersion := "2.8.1"

resolvers += "Local Maven" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
  "com.boundary" % "ordasity" % "0.2.0",
  "net.databinder" %% "dispatch-lift-json" % "0.8.5"
)
