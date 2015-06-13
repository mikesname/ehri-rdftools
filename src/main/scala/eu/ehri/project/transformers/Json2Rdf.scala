package eu.ehri.project.transformers

import java.io.{File, PrintStream}

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

object Json2Rdf {

  val jsonMapper = new ObjectMapper() with ScalaObjectMapper
  jsonMapper.registerModule(DefaultScalaModule)
  val jsonFactory: JsonFactory = new JsonFactory().setCodec(jsonMapper)

  val nameSpace: String = "http://ehri-project.eu/"
  val factory: ValueFactory = ValueFactoryImpl.getInstance()

  // Currently ignoring these types 'cos they're a little troublesome
  val ignoredTypes = Set(
    "systemEvent",
    "version"
  )

  def convert(src: BufferedSource, out: PrintStream, format: RDFFormat): Unit = {
    
    val rdfwriter: RDFWriter = Rio.createWriter(format, out)
    rdfwriter.handleNamespace("ehri", nameSpace)
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
        case Data(Some(id), Some(t), props, rels) if !ignoredTypes.contains(t) =>
          dataToGraph(id, t, props, rels)
      }
    } else Stream.empty[Model]
  }

  def dataToGraph(id: String, itype: String, props: Map[String, Any], rels: Map[String, List[(String, String)]]): Model = {
    val graph: Model = new LinkedHashModel()
    val typeURI: URI = factory.createURI(nameSpace, itype)
    val itemURI: URI = factory.createURI(nameSpace, s"$itype#$id")
    graph.add(itemURI, RDF.TYPE, typeURI)

    def addLiteral(key: String, literal: Any) = literal match {
      case value: Int =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case value: String =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case value: Long =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case value: Float =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case value: Double =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case value: Boolean =>
        graph.add(itemURI, factory.createURI(nameSpace, key), factory.createLiteral(value))
      case _ =>
    }

    props.foreach {
      case (key, list: List[_]) => list.foreach(v => addLiteral(key, v))
      case (key, value) => addLiteral(key, value)
    }

    rels.foreach { case (label, set) =>
      val relProp: URI = factory.createURI(nameSpace, label)
      set.collect {
        case (oid, otype) =>
          graph.add(itemURI, factory.createURI(nameSpace, label), factory.createURI(nameSpace, s"$otype#$oid"))
      }
    }

    graph
  }

  def main(args: Array[String]) = {

    val usage = "json2rdf [-i infile] [-f format]"

    val arglist = args.toList
    type OptionMap = Map[Symbol, String]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      def isSwitch(s: String) = s(0) == '-'
      list match {
        case Nil => map
        case "-i" :: value :: tail =>
          nextOption(map ++ Map('input -> value), tail)
        case "-f" :: value :: tail =>
          nextOption(map ++ Map('format -> value), tail)
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
      val fmt = options.getOrElse('format, "ttl")
      try {
        val rdfFormat: RDFFormat = Rio.getWriterFormatForFileName("name." + fmt)
        convert(src, System.out, rdfFormat)
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
