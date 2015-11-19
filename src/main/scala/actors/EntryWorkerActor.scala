package actors

import java.net.URL
import java.util.function.Consumer

import actors.EntryWorkerActor.{EntryInfo, FetchEntries, FetchEntryInfo}
import akka.actor.{Props, Actor}
import db.{RelTypes, Labels, DbOperations, EmbeddedDatabaseService}
import org.jsoup.Jsoup
import org.neo4j.graphdb.Node
import scala.util.{Success, Failure, Try}
import scala.concurrent.duration._

object EntryWorkerActor {

  case class FetchEntryInfo(entryId: String, nodeId: Long)

  case class EntryList(data: Array[String])

  case class EntryInfo(author: String, topic: String)

  object FetchEntries

  def props = Props(new EntryWorkerActor)

}

class EntryWorkerActor extends Actor with EmbeddedDatabaseService with DbOperations {

  import db.ds
  import utils.UrlConverters._
  import context.dispatcher

  def fetchEntryInfo(url: URL): Option[EntryInfo] = Try(Jsoup.parse(url, 3000)) match {
    case Success(element) =>
      val author: String = element.select("ul#entry-list li").attr("data-author").trim
      val title: String = element.select("#title").attr("data-title").trim
      Some(EntryInfo(author, title))
    case Failure(ex) =>
      println(ex)
      None
  }

  self ! FetchEntries

  override def receive: Receive = {
    case FetchEntryInfo(entryId, nodeId) =>
      if (!entryId.isEmpty)
      fetchEntryInfo(s"https://eksisozluk.com/entry/${entryId.substring(1)}") match {
        case Some(info) => withTx {
          val user = findUser(info.author) getOrElse createUser(info.author)
          val entryNode: Node = findEntry(entryId).get
          entryNode.setProperty("topic", info.topic)
          user.createRelationshipTo(entryNode, RelTypes.AUTHORED)
          println(s"$entryId is authored by ${info.author}")
        }
        case None =>
    }

    case FetchEntries => withTx {
      val entries = database.findNodes(Labels.ENTRY)
      entries.forEachRemaining(new Consumer[Node] {
        override def accept(t: Node): Unit = {
          if (!t.hasRelationship(RelTypes.AUTHORED)) {
            self ! FetchEntryInfo(t.getProperty("eid").asInstanceOf[String], t.getId)
          }
        }
      })
    }
    context.system.scheduler.scheduleOnce(4.minutes, self, FetchEntries)
  }
}
