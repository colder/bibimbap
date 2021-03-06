package bibimbap
package bibtex

import bibimbap.strings._

object BibTeXEntryTypes extends Enumeration {
  type BibTeXEntryType = Value
  val Article =       Value("article")
  val Book =          Value("book")
  val Booklet =       Value("booklet")
  val InBook =        Value("inbook")
  val InCollection =  Value("incollection")
  val InProceedings = Value("inproceedings")
  val Manual =        Value("manual")
  val MastersThesis = Value("mastersthesis")
  val Misc =          Value("misc")
  val PhDThesis =     Value("phdthesis")
  val Proceedings =   Value("proceedings")
  val TechReport =    Value("techreport")
  val Unpublished =   Value("unpublished")

  def withNameOpt(name: Option[String]): Option[BibTeXEntryType] =
    name.map(withNameOpt(_)).flatten

  def withNameOpt(name: String): Option[BibTeXEntryType] = try {
    Some(withName(name))
  } catch {
    case e: Throwable =>
      None
  }

  case class OneOf(fs: String*) {
    val set = fs.toSet
    def satisfiedBy(fields: Set[String]): Boolean = !(set & fields).isEmpty

    def toFields = fs.toList
  }

  import language.implicitConversions
  implicit def strToOneOf(str: String) = OneOf(str)

  val requiredFieldsFor = Map[BibTeXEntryType, List[OneOf]](
    Article         -> List("title", "author", "journal", "year"),
    Book            -> List("title", OneOf("author", "editor"), "publisher", "year"),
    Booklet         -> List("title"),
    InBook          -> List("title", OneOf("author", "editor"), OneOf("chapter", "pages"), "publisher", "year"),
    InCollection    -> List("title", "author", "booktitle", "year"),
    InProceedings   -> List("title", "author", "booktitle", "year"),
    Manual          -> List("title"),
    MastersThesis   -> List("title", "author", "school", "year"),
    Misc            -> List(),
    PhDThesis       -> List("title", "author", "school", "year"),
    Proceedings     -> List("title", "year"),
    TechReport      -> List("title", "author", "institution", "year"),
    Unpublished     -> List("title", "author", "note")
  ).withDefaultValue(List())

  def requiredFieldsFor(otpe: Option[BibTeXEntryType]): List[OneOf] = otpe.map(requiredFieldsFor).getOrElse(List())

  val optionalFieldsFor = Map(
    Article         -> List("volume", "number", "pages", "month", "note", "key"),
    Book            -> List("volume", "series", "address", "edition", "month", "note", "key", "pages"),
    Booklet         -> List("author", "howpublished", "address", "month", "year", "note", "key"),
    InBook          -> List("volume", "series", "address", "edition", "month", "note", "key"),
    InCollection    -> List("editor", "volume", "number", "series", "type", "chapter", "pages", "address", "edition", "month", "note", "key"),
    InProceedings   -> List("editor", "volume", "number", "series", "pages", "address", "month", "organization", "publisher", "note", "key"),
    Manual          -> List("author", "organization", "edition", "address", "year", "month", "note", "key"),
    MastersThesis   -> List("address", "month", "note", "key"),
    Misc            -> List("author", "howpublished", "title", "month", "year", "note", "key"),
    PhDThesis       -> List("address", "month", "note", "key"),
    Proceedings     -> List("editor", "volume", "number", "series", "address", "month", "publisher", "organization", "note", "key"),
    TechReport      -> List("type", "number", "address", "month", "note", "key"),
    Unpublished     -> List("month", "year", "key")
  ).withDefaultValue(List())

  def optionalFieldsFor(otpe: Option[BibTeXEntryType]): List[String] = otpe.map(optionalFieldsFor).getOrElse(List())

  def relevantFieldsFor(otpe: Option[BibTeXEntryType]): List[String] =
    requiredFieldsFor(otpe).flatMap(_.toFields) ++ optionalFieldsFor(otpe)

  val allStdFields = Set("address", "abstract", "annote", "author",
      "booktitle", "chapter", "crossref", "edition", "editor", "eprint",
      "howpublished", "institution", "journal", "key", "month", "note", "number",
      "organization", "pages", "publisher", "school", "series", "title", "type",
      "url", "volume", "year")
}

case class InconsistentBibTeXEntry(msg: String) extends Exception(msg)

// This datatypes and all the following ones assume crossrefs have been
// "resolved" into all entries.
case class BibTeXEntry(tpe: Option[BibTeXEntryTypes.BibTeXEntryType],
                       key: Option[String],
                       fields: Map[String, MString],
                       seqFields: Map[String, Seq[MString]]) extends Serializable {

  lazy val requiredFields = BibTeXEntryTypes.requiredFieldsFor(tpe)
  lazy val optionalFields = BibTeXEntryTypes.optionalFieldsFor(tpe)
  lazy val stdFields      = BibTeXEntryTypes.relevantFieldsFor(tpe)

  // convenience fields
  val address      : Option[MString] = fields.get("address")
  val annote       : Option[MString] = fields.get("annote")
  val authors      : Seq[MString]    = seqFields.getOrElse("author", Seq.empty)
  val booktitle    : Option[MString] = fields.get("booktitle")
  val chapter      : Option[MString] = fields.get("chapter")
  val edition      : Option[MString] = fields.get("edition")
  val editors      : Seq[MString]    = seqFields.getOrElse("editor", Seq.empty)
  val eprint       : Option[MString] = fields.get("eprint")
  val howpublished : Option[MString] = fields.get("howpublished")
  val institution  : Option[MString] = fields.get("institution")
  val journal      : Option[MString] = fields.get("journal")
  val month        : Option[MString] = fields.get("month")
  val note         : Option[MString] = fields.get("note")
  val number       : Option[MString] = fields.get("number")
  val organization : Option[MString] = fields.get("organization")
  val pages        : Option[MString] = fields.get("pages")
  val publisher    : Option[MString] = fields.get("publisher")
  val school       : Option[MString] = fields.get("school")
  val series       : Option[MString] = fields.get("series")
  val title        : Option[MString] = fields.get("title")
  val trType       : Option[MString] = fields.get("trType")
  val url          : Option[MString] = fields.get("url")
  val volume       : Option[MString] = fields.get("volume")
  val year         : Option[MString] = fields.get("year")
  val link         : Option[MString] = fields.get("link")
  val doi          : Option[MString] = fields.get("doi")
  val dblp         : Option[MString] = fields.get("dblp")
  val keyField     : Option[MString] = fields.get("key")

  lazy val entryMap = {
    fields ++ seqFields.mapValues(seq => MString.fromJava(seq.map(_.toJava).mkString(" and ")))
  }

  val allFields = fields.keySet ++ seqFields.keySet

  // Checks whether a bibtexentry may be the same with another
  def like(that: BibTeXEntry): Boolean = {
    def compField(a: Option[MString], b: Option[MString]) = (a,b) match {
      case (Some(aa), Some(bb)) =>
        aa.toJava == bb.toJava
      case _ =>
        false
    }

    if (this == that) {
      true
    } else if (compField(this.doi, that.doi)) {
      true
    } else if (compField(this.dblp, that.dblp)) {
      true
    } else if (this.getKey == that.getKey) {
      true
    } else if (this.generateKey == that.generateKey) {
      true
    } else if (compField(this.title, that.title)) {
      // Let's make sure by checking another criteria
      compField(this.year, that.year) ||
      compField(this.journal, that.journal) ||
      compField(this.booktitle, that.booktitle)
    } else {
      false
    }
  }

  def isValid: Boolean = {
    val missingReqFields = requiredFields.filter(!_.satisfiedBy(allFields))

    missingReqFields.isEmpty
  }

  def getType: BibTeXEntryTypes.BibTeXEntryType = tpe.getOrElse(BibTeXEntryTypes.Misc)
  def getKey: String = key.getOrElse(generateKey)

  def generateKey: String = {
    val commonWords = Set("", "in", "the", "a", "an", "of", "for", "and", "or", "by", "on", "with")

    def isBibTeXFriendly(c : Char) : Boolean = (
      (c >= 'A' && c <= 'Z') ||
      (c >= 'a') && (c <= 'z') ||
      (c >= '0') && (c <= '9')
    )

    def camelcasify(str : MString) : Seq[String] = {
      str.toJava.split(" ")
        .map(bit => MString.javaToASCII(bit).filter(isBibTeXFriendly))
        .filterNot(_.isEmpty)
        .map(_.toLowerCase)
        .filterNot(commonWords)
        .map(_.capitalize)
    }

    def lastFromPerson(person : MString) : String = {
      val lastBit = MString.fromJava(person.toJava.split(" ").last)
      lastBit.toASCII.filter(isBibTeXFriendly)
    }

    val persons   = if(!authors.isEmpty) authors else editors
    val lastnames = if(persons.size > 3) {
      lastFromPerson(persons(0)) + "ETAL"
    } else {
      persons.map(lastFromPerson).mkString("")
    }

    val yr = year match {
      case Some(y) => {
        val last = y.toJava.toInt % 100
        if(last < 10) "0" + last else last.toString
      }
      case None => ""
    }

    val title = this.title.map(t =>
      camelcasify(t).take(6).mkString("")
    ).getOrElse("")

    lastnames + yr + title
  }

  // Tries to shorten the first names (whatever that means).

  def inlineString: String = {

    def shortenName(name : String) : String = {
      val elements = name.split(" ").filterNot(_.isEmpty)
      if (elements.size > 1) {
        elements.dropRight(1).map(e => e(0) + ".").mkString("") + elements.last
      } else {
        name
      }
    }

    val (persons,areEditors) = if(!authors.isEmpty) {
      (authors, false)
    } else {
      (editors, true)
    }

    val personString = if(persons.size > 4) {
      shortenName(persons.head.toJava) + " et al."
    } else {
      persons.map(p => shortenName(p.toJava)).mkString(", ")
    }

    val names = if(areEditors) (personString + " ed.") else personString

    val title = "\"" + this.title.map(_.toJava).getOrElse("?") + "\""

    val where =
      booktitle.map(_.toJava).getOrElse(
        journal.map(_.toJava).getOrElse(
          school.map(_.toJava).getOrElse(
            howpublished.map(_.toJava).getOrElse("?"))))

    val year = this.year.map(_.toJava).getOrElse("?")

    names + ", " + title + ", " + where + ", " + year
  }

  override def toString = toStringWithKey(getKey)

  private val preferredDisplayingOrder : Seq[String] = List("title", "author", "editor", "booktitle", "journal", "year")
  def toStringWithKey(key : String) : String = {
    val buffer = new StringBuilder
    buffer.append("@" + getType + "{" + key + ",\n")

    def printOptField(name : String, value : Option[MString]) {
      value.foreach(content => {
        buffer.append("  ")
        buffer.append("%12s = {".format(name))
        buffer.append(content.toLaTeX)
        buffer.append("},\n")
      })
    }

    def printSeqField(name : String, values : Seq[MString]) {
      if(!values.isEmpty) {
        buffer.append("  ")
        buffer.append("%12s = {".format(name))
        buffer.append(values.map(_.toLaTeX).mkString(" and "))
        buffer.append("},\n")
      }
    }

    var remaining : Set[String] = allFields
    def printSubset(subset : Traversable[String]) {
      for(field <- subset if remaining(field)) {
        if(seqFields contains field) {  
          printSeqField(field, seqFields(field))
        } else {
          printOptField(field, fields.get(field))
        }
        remaining -= field
      }
    }

    printSubset(preferredDisplayingOrder)
    printSubset(requiredFields.flatMap(_.toFields).toSeq.sorted)
    printSubset(optionalFields.toSeq.sorted)
    printSubset(allFields.toSeq.sorted)

    buffer.dropRight(2).append("\n}").toString
  }

  def display(out: String => Unit, fieldFormatter: String => String, errorFormatter: String => String) {
    val missingVal = ""

    out("  Entry type : "+tpe.getOrElse(missingVal))
    out("  Entry key  : "+getKey)
    out("")
    out("  Required fields:")
    for (f <- requiredFields.flatMap(_.toFields)) {
      if (entryMap contains f) {
        out(("   "+fieldFormatter("%12s")+" = %s").format(f, entryMap(f).toJava))
      } else {
        out(("   "+errorFormatter("%12s")+" = %s").format(f, missingVal))
      }
    }
    out("")
    out("  Optional fields:")
    for (f <- optionalFields) {
      out(("   "+fieldFormatter("%12s")+" = %s").format(f, entryMap.get(f).map(_.toJava).getOrElse(missingVal)))
    }

    val extraFields = entryMap.keySet -- BibTeXEntryTypes.allStdFields
    if (!extraFields.isEmpty) {
      out("")
      out("  Extra fields:")
      for (f <- extraFields) {
        out(("   "+fieldFormatter("%12s")+" = %s").format(f, entryMap(f).toJava))
      }
    }
  }
}

object BibTeXEntry {
  def fromEntryMap(tpe: Option[BibTeXEntryTypes.BibTeXEntryType],
                   key: Option[String],
                   map : Map[String,MString],
                   onError: String => Unit) : Option[BibTeXEntry] = {
    try {
      val isSeqField = Set("author", "editor")

      var fields    = Map[String, MString]()
      var seqFields = Map[String, Seq[MString]]()

      for ((field, value) <- map) {
        if (isSeqField(field)) {
          seqFields += field -> value.toJava.split(" and ").map(MString.fromJava _).toSeq
        } else {
          fields += field -> value
        }
      }

      Some(BibTeXEntry(tpe, key, fields, seqFields))
    } catch {
      case InconsistentBibTeXEntry(msg) =>
        onError(msg)
        None
    }
  }
}
