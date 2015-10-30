package actors

import java.net.URL

import actors.EntryWorkerActor.TrackFavorite
import actors.UserScannerActor.ScanUser
import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import db.{DbOperations, EmbeddedDatabaseService}
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

sealed trait Command
case object Initialize extends Command

object UserScannerActor {

  case class ScanUser(username: String, page: Int = 1) extends Command

  case class FetchUrl(url: URL, selector: String) extends Command

  case class EntryList(data: Array[String])

  def props: Props = Props(new UserScannerActor)

  def fetchUrl(url: URL): Try[EntryList] = Try(new EntryList(Jsoup.parse(url, 3000).select("ul.topic-list a span").html().split("\n")))

}

class UserScannerActor extends Actor with EmbeddedDatabaseService with DbOperations with ActorLogging {
  import utils.UrlConverters._

  val entryWorkers = context.actorOf(EntryWorkerActor.props.withRouter(RoundRobinPool(5)))

  override def receive: Receive = {
    case ScanUser(u, page: Int) => UserScannerActor.fetchUrl(s"https://eksisozluk.com/basliklar/istatistik/${u}/favori-entryleri?p=${page}") match {

      case Success(entryList) =>

        def processEntries():Boolean =  {
          for(entryId <- entryList.data) {
            withTx {
              findEntry(entryId) match {
                case Some(x) => return false
                case None => entryWorkers ! TrackFavorite(u, entryId)
              }
            }
          }

          entryList.data.length > 0
        }

        if (processEntries())
          self ! ScanUser(u, page+1)

      case Failure(e) => print("failed " + e.getMessage)
    }
    case Initialize => print("initialized")
  }
}
