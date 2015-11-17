package actors

import java.net.URL

import actors.PersistenceActor.PersistFavorited
import actors.UserScannerActor.{EntryInfo, FetchEntryInfo, ScanPage}
import akka.actor.{Actor, ActorLogging, Props}
import db.{DbOperations, EmbeddedDatabaseService, RelTypes}
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

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
  import context.dispatcher

  def fetchFavorites(url: URL): Try[Array[String]] = Try(Jsoup.parse(url, 3000).select("ul.topic-list a span").html().split("\n"))

  def fetchEntryInfo(url: URL): Try[EntryInfo] = Try(EntryInfo(Jsoup.parse(url, 3000).select("ul#entry-list li").attr("data-author").trim))

  context.system.scheduler.schedule(0.milliseconds, 5.minutes, self, ScanPage(1))
  val persistenceActor = context.actorSelection("akka://main/user/persistence")

  override def receive: Receive = {
    case ScanPage(page: Int) =>
      fetchFavorites(s"https://eksisozluk.com/basliklar/istatistik/$username/favori-entryleri?p=$page") match {
        case Success(entryList) =>
          val nonEmpty = entryList.takeWhile(!_.isEmpty)
          println(s"entry count user $username for page $page is ${nonEmpty.length}")

          if (nonEmpty.nonEmpty) {
            withTx {
              lazy val userNode = findUser(username)
              if (userNode.isDefined) {
                val uncommitted = entryList.takeWhile(!isFavoritedBefore(userNode.get, _))
                uncommitted.foreach(persistenceActor ! PersistFavorited(username, _))
                if (uncommitted.length == entryList.length)
                  self ! ScanPage(page + 1)
              } else {
                println(s"$username is not created")
                nonEmpty.foreach(persistenceActor ! PersistFavorited(username, _))
                self ! ScanPage(page + 1)
              }
            }
          }

        case Failure(e) => print(s"failed ${e.getMessage}")
      }
    case FetchEntryInfo(entryId) =>
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
