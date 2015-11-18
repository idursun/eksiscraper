import actors.{EntryInfoFetcherActor, PersistenceActor, RecommenderActor, UserScannerActor}
import akka.actor.ActorSystem
import db.{DbOperations, EmbeddedDatabaseService}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with EmbeddedDatabaseService with DbOperations {

  lazy val system = ActorSystem("main")
  system.actorOf(PersistenceActor.props, "persistence")
  system.actorOf(EntryInfoFetcherActor.props, "entryInfoFetcher")

  val seedUsers = List("ssg", "teo", "thex",  "jun%20misugi", "immanuel%20tolstoyevski", "fridanin%20parcalanmis%20omurgasi", "turing")
  for (user <- seedUsers) system.actorOf(UserScannerActor.props(user), user)

  system.actorOf(RecommenderActor.props, "recommender")

  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()

  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)

}
