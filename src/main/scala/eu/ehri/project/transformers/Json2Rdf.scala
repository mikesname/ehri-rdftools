package eu.ehri.project.transformers

import java.io.{OutputStream, File}

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.openrdf.model._
import org.openrdf.model.impl.{LinkedHashModel, ValueFactoryImpl}
import org.openrdf.model.vocabulary.RDF
import org.openrdf.rio.{RDFFormat, RDFWriter, Rio, UnsupportedRDFormatException}

import scala.collection.JavaConversions._
import scala.io.BufferedSource


case class Data(
  id: Option[String],
  `type`: Option[String],
  data: Map[String, Any],
  relationships: Map[String, List[(String, String)]]
)

case class Json2Rdf(nameSpace: String, prefix: String, format: RDFFormat) {

  val jsonMapper = new ObjectMapper() with ScalaObjectMapper
  jsonMapper.registerModule(DefaultScalaModule)
  val jsonFactory: JsonFactory = new JsonFactory().setCodec(jsonMapper)

  val factory: ValueFactory = ValueFactoryImpl.getInstance()

  // Cache certain URIs to create fewer objects. These are used for
  // properties and item types where the data has a low cardinality
  private object URIStore {
    private val propCache = collection.mutable.Map[String, URI]()

    private def createNewUri(s: String) = factory.createURI(nameSpace, s)

    def getUri(s: String) = propCache.getOrElseUpdate(s, createNewUri(s))
  }

  def convert(src: BufferedSource, out: OutputStream): Unit = {

    val rdfwriter: RDFWriter = Rio.createWriter(format, out)
    rdfwriter.handleNamespace(prefix, nameSpace)
    rdfwriter.startRDF()

    convertStream(src).foreach { graph =>
      graph.foreach(rdfwriter.handleStatement)
    }

    rdfwriter.endRDF()
  }

  def convertStream(src: BufferedSource): Stream[Model] = {
    val parser: JsonParser = jsonFactory.createParser(src.reader())
    val firstToken: JsonToken = parser.nextValue()

    if (!parser.isExpectedStartArrayToken) {
      throw new Exception("Expecting a JSON array, instead first token was: " + firstToken)
    }
    if (parser.nextValue() != JsonToken.END_ARRAY) {

      jsonMapper.readValues(parser, classOf[Data]).toStream.collect {
        case Data(Some(id), Some(t), props, rels) =>
          dataToGraph(id, t, props, rels)
      }
    } else Stream.empty[Model]
  }

  def dataToGraph(id: String, itype: String, props: Map[String, Any], rels: Map[String, List[(String, String)]]): Model = {
    val graph: Model = new LinkedHashModel()
    val typeURI: URI = URIStore.getUri(itype)
    val itemURI: URI = factory.createURI(nameSpace, s"$itype#$id")
    graph.add(itemURI, RDF.TYPE, typeURI)

    def addLiteral(key: String, literal: Any) = literal match {
      case value: Int =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case value: String =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case value: Long =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case value: Float =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case value: Double =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case value: Boolean =>
        graph.add(itemURI, URIStore.getUri(key), factory.createLiteral(value))
      case _ =>
    }

    props.foreach {
      case (key, list: List[_]) => list.foreach(v => addLiteral(key, v))
      case (key, value) => addLiteral(key, value)
    }

    rels.foreach { case (label, set) =>
      set.collect {
        case (oid, otype) =>
          graph.add(itemURI, URIStore.getUri(label), factory.createURI(nameSpace, s"$otype#$oid"))
      }
    }

    graph
  }
}

object Json2Rdf {
  def main(args: Array[String]) = {

    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "-i" :: value :: tail =>
          nextOption(map ++ Map('input -> value), tail)
        case "-f" :: value :: tail =>
          nextOption(map ++ Map('format -> value), tail)
        case "-n" :: value :: tail =>
          nextOption(map ++ Map('ns -> value), tail)
        case "-p" :: value :: tail =>
          nextOption(map ++ Map('prefix -> value), tail)
        case option :: tail =>
          throw new IllegalArgumentException("Unknown option " + option)
      }
    }
    try {
      val options = nextOption(Map('format -> "ttl"), arglist)

      val src = options.get('input) match {
        case Some("-") | None => io.Source.stdin
        case Some(fn) if new File(fn).exists() => io.Source.fromFile(fn)
        case Some(fn) if !new File(fn).exists() =>
          throw new IllegalArgumentException(s"Input file $fn does not exist!")
      }
      val ns = options.getOrElse('ns, "http://ehri-project.eu/")
      val prefix = options.getOrElse('prefix, "ehri")
      val fmt = options.getOrElse('format, "ttl")
      try {
        val rdfFormat: RDFFormat = Rio.getWriterFormatForFileName("name." + fmt)
        Json2Rdf(ns, prefix, rdfFormat).convert(src, System.out)
      } catch {
        case e: UnsupportedRDFormatException =>
          throw new IllegalArgumentException("Unable to write RDF format: " + fmt)
      }
    } catch {
      case e: IllegalArgumentException =>
        System.err.println(e.getMessage)
        System.exit(1)
    }
  }
}
