package bibimbap
package dblp

import bibimbap.data._

import scala.io.Source

import java.net.URL
import java.net.URLEncoder
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

import com.codahale.jerkson.Json
import com.codahale.jerkson.ParsingException
import com.codahale.jerkson.AST._

class DBLPModule(settings : Settings) extends SearchModule(settings) {
  module =>

  val name = "Remote DBLP access"

  val keyword = "dblp"

  val dataSourceName = "DBLP"

  override val moreActions  = Seq(searchAction)

  private val info : Any=>Unit = settings.logger.info
  private val warn : Any=>Unit = settings.logger.warn

  private val searchURLPrefix  = "http://www.dblp.org/search/api/?q="
  private val searchURLPostfix = "&h=10&c=4&f=0&format=json"

  lazy val searchAction = new Action[SearchResult]("search") {
    val description = "Search for records on DBLP (remotely)."

    def run(args : String*) : SearchResult = {
      try {
        val pattern = URLEncoder.encode(args.mkString(" "), "UTF-8")
        val url = new URL(searchURLPrefix + pattern + searchURLPostfix)
        val urlCon = url.openConnection()
        urlCon.setConnectTimeout(3000)
        urlCon.setReadTimeout(3000)
        val content = Source.fromInputStream(urlCon.getInputStream)
        val text = content.getLines.mkString(" ")
        // info("JSON : " + text)
        extractJSONRecords(text).flatMap(recordToResult)
      } catch {
        case ce : ConnectException => {
          warn("Connection error: " + ce.getLocalizedMessage)
          Nil
        }
        case ste : SocketTimeoutException => {
          warn("Network error: " + ste.getLocalizedMessage)
          Nil
        }
        case uhe : UnknownHostException => {
          warn("Network error (unknown host): " + uhe.getLocalizedMessage)
          Nil
        }
      }
    }
  }

  private def extractJSONRecords(text : String) : Seq[JValue] = {
    try {
      (Json.parse[JValue](text) \\ "hit").flatMap(hit => hit match {
        case JArray(elems) => elems
        case _ => Nil
      })
    } catch {
      case pe : ParsingException => {
        warn("Remote responded with malformed JSON (this is known to happen when there are no results).")
        Nil
      }
    }
  }

  private val unknown : String = "???"
  private val ConfVenueStr = """(.*) (\d\d\d\d):([\d- ]*)""".r
  private val JourVenueStr = """(.*) (\d+)\((\d+)\):([\d- ]*) \((\d\d\d\d)\)""".r
"Commun. ACM (CACM) 55(2):103-111 (2012)"
  private def recordToResult(record : JValue) : Option[SearchResultEntry] = {
    def yr2yr(year : Option[String]) : Option[Int] = try {
      year.map(_.trim.toInt)
    } catch {
      case nfe : NumberFormatException => None
    }

    (record \ "title") match {
      case obj : JObject => {
        val authors : Seq[String] = (obj \ "dblp:authors" \ "dblp:author") match {
          case JArray(elems) => elems.collect { case JString(str) => str }
          case JString(single) => Seq(single)
          case _ => Nil
        }

        val title : String = (obj \ "dblp:title" \ "text") match {
          case JString(str) => cleanupTitle(str)
          case _ => unknown
        }

        val link : Option[String] = (obj \ "dblp:title" \ "@ee") match {
          case JString(str) => Some(str)
          case _ => None
        }

        val year : Option[Int] = (obj \ "dblp:year") match {
          case JInt(bigInt) => Some(bigInt.toInt)
          case _ => None
        }

        // Some of the info is entry type specific, so we now check the type.
        (obj \ "dblp:type") match {
          case JString("inproceedings") => {
            val (venue,venueYear,pages) = (obj \ "dblp:venue" \ "text") match {
              case JString(vs @ ConfVenueStr(v, y, p)) => {
                // info("Venue string : " + vs)
                // info("Breakdown : [" + v + "], [" + y + "], [" + p + "]")
                (Some(v), Some(y), Some(cleanupPages(p)))
              }
              // case JString(os) => warn("Could not extract venue information from string [" + os + "]."); (None, None, None)
              case _ => (None, None, None)
            }

            val entry = new InProceedings(
              authors,
              title,
              venue.getOrElse(unknown),
              yr2yr(venueYear).getOrElse(year.getOrElse(0)),
              pages = pages
            )
            Some(SearchResultEntry(entry, () => entry, link, module.keyword))
          }

          case JString("article") => {
            // info("In article : " + (obj \ "dblp:venue" \ "text"))
            val (jour,vol,num,pgs,yr) = (obj \ "dblp:venue" \ "text") match {
              case JString(vs @ JourVenueStr(j,v,n,p,y)) => {
                (Some(cleanupJournal(j)), Some(v), Some(n), Some(cleanupPages(p)), Some(y))
              }
              // case JString(os) => warn("Could not extract venue information from string [" + os + "]."); (None, None, None, None, None)
              case _ => (None, None, None, None, None)
            }

            val entry = new Article(
              authors,
              title,
              jour.getOrElse(unknown),
              yr2yr(yr).getOrElse(year.getOrElse(0)),
              volume = vol,
              number = num,
              pages = pgs
            )
            Some(SearchResultEntry(entry, () => entry, link, module.keyword))
          }

          case JString(other) => {
            // info("Other type : \"" + other + "\"")
            None
          }

          case _ => None
        }
      }
      case _ => None
    }
  }

  private def cleanupTitle(title : String) : String = {
    val trimmed = title.trim
    if(trimmed.endsWith(".")) trimmed.substring(0, trimmed.length - 1) else trimmed
  }

  private lazy val JournalAbbr = """(.*) \(([A-Z]+)\)""".r
  private def cleanupJournal(journal : String) : String = {
    journal.trim match {
      case JournalAbbr(_, abbr) => abbr
      case other => other
    }
  }

  val Pages = """(\d+)([\s-]+)(\d+)""".r
  private def cleanupPages(pages : String) : String = pages.trim match {
    case Pages(start, _, end) => start + "--" + end
    case other => other
  }
}
