name := "eskiScraper"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Neo4j" at "http://m2.neo4j.org/content/repositories/releases/"

resolvers += "central" at "http://repo1.maven.org/maven2/"

libraryDependencies ++= {
  val akkaV = "2.4.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.jsoup" % "jsoup" % "1.7.2",
    "org.neo4j" % "neo4j" % "2.3.0"
  )
}
