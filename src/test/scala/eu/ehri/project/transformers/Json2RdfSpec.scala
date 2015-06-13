package eu.ehri.project.transformers

import java.io.{PrintStream, ByteArrayOutputStream}

import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.{Statement, Model}
import org.openrdf.rio.RDFFormat
import org.scalatest.{Matchers, FlatSpec}
import scala.collection.JavaConversions._


import scala.io.Source

class Json2RdfSpec extends FlatSpec with Matchers {
  "Json2Rdf" should "serialize to a triple stream" in {
    val src = Source.fromURL(getClass.getResource("/test.json"))
    val tripleSets: List[Model] = Json2Rdf.convertStream(src).toList
    assert(tripleSets.size === 1)
    val statements: List[Statement] = tripleSets.head.toList
    assert(statements.size === 5)
    val isA = statements(0)
    val prop1 = statements(1)
    val prop2a = statements(2)
    val rel = statements(4)

    assert(isA.getPredicate == RDF.TYPE)
    assert(isA.getSubject.toString === "http://ehri-project.eu/bar#foo")
    assert(prop1.getPredicate.toString === "http://ehri-project.eu/key1")
    assert(prop1.getObject.stringValue() === "value1")
    assert(prop2a.getPredicate.toString === "http://ehri-project.eu/key2")
    assert(prop2a.getObject.stringValue() === "value2a")
    assert(rel.getPredicate.toString === "http://ehri-project.eu/someRel")
    assert(rel.getObject.toString === "http://ehri-project.eu/a-type#an-id")
  }

  "Json2Rdf" should "serialize to Turtle with correct array properties" in {
    val src = Source.fromURL(getClass.getResource("/test.json"))
    val baos = new ByteArrayOutputStream()
    val printStream = new PrintStream(baos)
    Json2Rdf.convert(src, printStream, RDFFormat.TURTLE)
    val out = baos.toString("UTF-8")
    assert(out contains "ehri:key2 \"value2a\" , \"value2b\"")
  }
}
