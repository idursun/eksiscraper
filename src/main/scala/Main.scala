import actors.UserScannerActor
import akka.actor.ActorSystem
import db.{DbOperations, EmbeddedDatabaseService}

import scala.concurrent.Await
import scala.concurrent.duration._


case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Main extends App with EmbeddedDatabaseService with DbOperations {

  lazy val system = ActorSystem("main")

  val seeds = List("teo", "ssg", "thex")
  for(a <- seeds) system.actorOf(UserScannerActor.props(a))
//  val actor = system.actorOf(UserScannerActor.props("teo"))


  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()

  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)

}
