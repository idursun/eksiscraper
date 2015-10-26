package actors

import java.net.URL

import actors.UserScannerActor.ScanUser
import akka.actor.{Actor, Props}
import org.jsoup.Jsoup

import scala.util.{Failure, Success, Try}

sealed trait Command
case object Initialize extends Command

object UserScannerActor {

  case class ScanUser(username: String) extends Command
  case class FetchUrl(url:URL, selector: String) extends Command
  case class EntryList(data: Array[String])

  def props: Props = Props(new UserScannerActor)
  def fetchUrl(url: URL): Try[EntryList] = Try(new EntryList(Jsoup.parse(url, 3000).select("ul.topic-list a span").html().split("\n")))

}

class UserScannerActor extends Actor {
  import utils.UrlConverters._

  override def receive: Receive = {
    case ScanUser(u) => UserScannerActor.fetchUrl(s"https://eksisozluk.com/basliklar/istatistik/${u}/favori-entryleri") match {
      case Success(entryList) => {
        entryList.data.foreach(println(_))
        Graph.db
      }
      case Failure(e) => print("failed " + e.getMessage)
    }
    case Initialize => print("initialized")
  }
}
