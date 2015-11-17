package actors

import java.net.URL
import java.util.function.Consumer

import actors.EntryInfoFetcherActor.{EntryInfo, DetectEntries, FetchEntryInfo}
import akka.actor.{Props, Actor}
import db.{RelTypes, Labels, DbOperations, EmbeddedDatabaseService}
import org.jsoup.Jsoup
import org.neo4j.graphdb.Node
import scala.util.{Success, Failure, Try}

object EntryInfoFetcherActor {

  case class FetchEntryInfo(entryId: String, nodeId: Long)

  case class EntryList(data: Array[String])

  case class EntryInfo(author: String)

  object DetectEntries

  def props = Props(new EntryInfoFetcherActor)

}

class EntryInfoFetcherActor extends Actor with EmbeddedDatabaseService with DbOperations {

  import db.ds
  import utils.UrlConverters._

  def fetchEntryInfo(url: URL): Try[EntryInfo] = Try(EntryInfo(Jsoup.parse(url, 3000).select("ul#entry-list li").attr("data-author").trim))

  self ! DetectEntries

  override def receive: Receive = {
    case FetchEntryInfo(entryId, nodeId) => fetchEntryInfo(s"https://eksisozluk.com/entry/${entryId.substring(1)}") match {
      case Success(info) => withTx {
        val user = findUser(info.author) getOrElse createUser(info.author)
        user.createRelationshipTo(findEntry(entryId).get, RelTypes.AUTHORED)
        println(s"$entryId is authored by ${info.author}")
      }
      case Failure(ex) => println(s"failed to get info for entry $entryId")
    }

    case DetectEntries => withTx {
      val entries = database.findNodes(Labels.ENTRY)
      entries.forEachRemaining(new Consumer[Node] {
        override def accept(t: Node): Unit = {
          if (!t.hasRelationship(RelTypes.AUTHORED)) {
            self ! FetchEntryInfo(t.getProperty("eid").asInstanceOf[String], t.getId)
          }
        }
      })
    }
    self ! DetectEntries
  }
}
