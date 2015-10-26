import java.io.File

import actors.UserScannerActor
import actors.UserScannerActor.ScanUser
import akka.actor.ActorSystem
import org.neo4j.graphdb.factory.GraphDatabaseFactory

case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Graph {
  lazy val db  = new GraphDatabaseFactory().newEmbeddedDatabase( new File("c:/data") )
}

object Main extends App {

  lazy val system = ActorSystem("main")
  val actor = system.actorOf(UserScannerActor.props)

  actor ! ScanUser("thex")

  system.awaitTermination()
  Graph.db.shutdown()

}
