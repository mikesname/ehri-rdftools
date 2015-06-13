name := """json2rdf"""

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.5.4",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.5.2",

    "org.openrdf.sesame" % "sesame-rio-api" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-turtle" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-ntriples" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-n3" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-trig" % "2.8.3",
    "org.openrdf.sesame" % "sesame-rio-rdfjson" % "2.8.3",

    // Prevents warning from openrdf...
    "org.slf4j" % "slf4j-simple" % "1.6.2",
    // Testing
    "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

mainClass in (Compile, run) := Some("eu.ehri.project.transformers.Json2Rdf")
mainClass in assembly := (mainClass in (Compile, run)).value

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(AssemblyPlugin.defaultShellScript))


