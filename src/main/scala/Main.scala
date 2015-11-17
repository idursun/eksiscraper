import actors.{PersistenceActor, UserScannerActor}
import akka.actor.ActorSystem
import db.{DbOperations, EmbeddedDatabaseService}

import scala.concurrent.Await
import scala.concurrent.duration._


case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Main extends App with EmbeddedDatabaseService with DbOperations {

  lazy val system = ActorSystem("main")
  system.actorOf(PersistenceActor.props, "persistence")

  val seedUsers = List("teo", "ssg", "thex", "sesshenn", "sarrus")
  for(user <- seedUsers) system.actorOf(UserScannerActor.props(user), user)

  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()

  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)

}
