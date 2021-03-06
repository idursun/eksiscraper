import actors.{EntryWorkerActor, PersistenceActor, RecommenderActor, UserScannerActor}
import akka.actor.ActorSystem
import db.{DbOperations, EmbeddedDatabaseService}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with EmbeddedDatabaseService with DbOperations {

  lazy val system = ActorSystem("main")
  system.actorOf(PersistenceActor.props, "persistence")
  system.actorOf(EntryWorkerActor.props, "entryWorker")

  val seedUsers = List("lord eddard stark", "merkuryel vapur", "duc de blangis", "sana meyilim var inboxina bak", "ketender tepetopuzu")
  for (user <- seedUsers) system.actorOf(UserScannerActor.props(user), user.replace(" ", "%20"))

  system.actorOf(RecommenderActor.props, "recommender")

  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()

  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)

}
