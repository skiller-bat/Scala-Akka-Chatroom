name := "akka-quickstart-scala"

version := "1.0"

//scalaVersion := "2.13.1"
scalaVersion := "3.2.2"

lazy val akkaVersion = "2.7.0"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-http" % "10.5.0-M1", //
  "com.typesafe.akka" %% "akka-remote" % akkaVersion, // added
  "io.aeron" % "aeron-driver" % "1.40.0", //
  "io.aeron" % "aeron-client" % "1.40.0", //
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
