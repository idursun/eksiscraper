package actors

import java.net.URL

import actors.UserScannerActor.{EntryInfo, FetchEntryInfo, ScanPage}
import akka.actor.{Actor, ActorLogging, Props}
import db.{DbOperations, EmbeddedDatabaseService, RelTypes}
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

object UserScannerActor {

  case class ScanPage(page: Int = 1)

  case class FetchEntryInfo(entryId: String)

  case class EntryList(data: Array[String])

  case class EntryInfo(author: String)

  def props(username: String): Props = Props(new UserScannerActor(username))
}

class UserScannerActor(val username: String) extends Actor with EmbeddedDatabaseService with DbOperations with ActorLogging {

  import db.ds
  import utils.UrlConverters._

  def fetchFavorites(url: URL): Try[Array[String]] = Try(Jsoup.parse(url, 3000).select("ul.topic-list a span").html().split("\n"))

  def fetchEntryInfo(url: URL): Try[EntryInfo] = Try(EntryInfo(Jsoup.parse(url, 3000).select("ul#entry-list li").attr("data-author").trim))

  override def receive: Receive = {
    case ScanPage(page: Int) =>
      fetchFavorites(s"https://eksisozluk.com/basliklar/istatistik/$username/favori-entryleri?p=$page") match {
        case Success(entryList) =>
          println(s"entry count user $username for page $page is ${entryList.length}")
          withTx {
            lazy val userNode = findUser(username) getOrElse createUser(username)
            var marked = false
            for (entry <- entryList if !entry.isEmpty && !isFavoritedBefore(userNode, entry)) {
              markFavorited(userNode, entry)
              self ! FetchEntryInfo(entry)
              marked = true
            }
            if (marked)
              self ! ScanPage(page + 1)
          }

        case Failure(e) => print(s"failed ${e.getMessage}")
      }
    case FetchEntryInfo(entryId) => {
      println(s"fetching entry $entryId")
      withTx {
        findEntry(entryId) match {
          case Some(node) => fetchEntryInfo(s"https://eksisozluk.com/entry/${entryId.substring(1)}") match {
            case Success(entryInfo) =>
              findUser(entryInfo.author) match {
                case Some(userNode) =>
                  userNode.createRelationshipTo(node, RelTypes.AUTHORED)
                case None => val userNode = createUser(entryInfo.author)
                  userNode.createRelationshipTo(node, RelTypes.AUTHORED)
              }
          }
          case None => println(s"entry $entryId not found")
        }
      }
    }
  }
}
