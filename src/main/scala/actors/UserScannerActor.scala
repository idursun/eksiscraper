package actors

import java.net.URL

import actors.UserScannerActor.ScanUser
import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import db.{DbOperations, EmbeddedDatabaseService}
import org.jsoup.Jsoup
import org.neo4j.graphdb.Node

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
    case ScanUser(username, page: Int) => {
      UserScannerActor.fetchUrl(s"https://eksisozluk.com/basliklar/istatistik/${username}/favori-entryleri?p=${page}") match {

        case Success(entryList) =>
          println(s"entry count user ${username} for ${page} is ${entryList.data.length}")
          def processEntries(user: Node, items: Array[String]): Boolean = {
            for (entryId <- items) {
              if (!markFavorited(user, entryId)) {
                return false
              }
              println(s"${username} favorited ${entryId}")
            }
            entryList.data.length > 1
          }

          var processed: Boolean = false
          withTx {
            val user: Node = findUser(username)
            processed = processEntries(user, entryList.data)
          }

          if (processed)
            self ! ScanUser(username, page+1)

        case Failure(e) => print(s"failed ${e.getMessage}")
      }

    }
    case Initialize => print("initialized")
  }

}
