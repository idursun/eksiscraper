package actors

import java.net.URL

import actors.PersistenceActor.PersistFavorited
import actors.UserScannerActor.ScanPage
import akka.actor.{Actor, ActorLogging, Props}
import db.{DbOperations, EmbeddedDatabaseService}
import org.jsoup.Jsoup

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object UserScannerActor {
  case class ScanPage(page: Int = 1)
  def props(username: String): Props = Props(new UserScannerActor(username))
}

class UserScannerActor(val username: String) extends Actor with EmbeddedDatabaseService with DbOperations with ActorLogging {

  def fetchFavorites(url: URL): Try[Array[String]] = Try(Jsoup.parse(url, 3000).select("ul.topic-list a span").html().split("\n"))

  import context.dispatcher
  import db.ds
  import utils.UrlConverters._

  context.system.scheduler.schedule(0.milliseconds, 10.minutes, self, ScanPage(1))
  val persistenceActor = context.actorSelection("akka://main/user/persistence")

  override def receive: Receive = {
    case ScanPage(page: Int) =>
      fetchFavorites(s"https://eksisozluk.com/basliklar/istatistik/${username.replace(" ", "%20")}/favori-entryleri?p=$page") match {
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
  }
}
