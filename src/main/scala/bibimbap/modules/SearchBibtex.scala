package bibimbap
package modules

import akka.actor._
import bibtex._
import strings._
import bibtex._

import scala.io.Source

class SearchBibtex(val repl: ActorRef, val console: ActorRef, val settings: Settings) extends Module
                                                                                      with LuceneRAMBackend
                                                                                      with LuceneSearchProvider {
  val name = "SearchBibtex"

  val source = "searchBibtex"

  override def receive: Receive = {
    case Command2("load", path) =>
      val parser = new BibTeXParser(Source.fromFile(path), console ! Error(_))

      for (entry <- parser.entries) {
        addEntry(entry, None)
      }

      sender ! CommandSuccess

    case Search(terms) =>
      sender ! search(terms)

    case msg =>
      super[Module].receive(msg)
  }

  val helpItems = Map(
    "load" -> HelpEntry("load <path>",  "loads <path> bibtex file into searchable cache")
  )
}