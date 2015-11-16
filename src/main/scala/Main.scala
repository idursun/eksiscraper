import actors.UserScannerActor
import actors.UserScannerActor.ScanPage
import akka.actor.ActorSystem
import db.{DbOperations, EmbeddedDatabaseService}

import scala.concurrent.Await
import scala.concurrent.duration._


case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Main extends App with EmbeddedDatabaseService with DbOperations {

  lazy val system = ActorSystem("main")
  import system.dispatcher
  val actor = system.actorOf(UserScannerActor.props("teo"))

  system.scheduler.schedule(0.milliseconds, 5.minutes, actor, ScanPage(1))

  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()

  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)

}
