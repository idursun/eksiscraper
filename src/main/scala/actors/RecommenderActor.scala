package actors

import actors.RecommenderActor.Recommend
import akka.actor.{Props, ActorIdentity, Identify, Actor}
import db.{DbOperations, EmbeddedDatabaseService}
import org.neo4j.graphdb.Node

object RecommenderActor {

  object Recommend

  def props = Props(new RecommenderActor)

}

class RecommenderActor extends Actor with EmbeddedDatabaseService with DbOperations {
  import db.ds

  self ! Recommend

  override def receive: Receive = {
    case Recommend => withTx {
      val results = database.execute("match (u:USER)-[:AUTHORED]->()<-[:FAVORITED]-(k:USER) with u, count(*) as c where c > 10 return u,c")
      while (results.hasNext) {
        val row = results.next()
        val username = row.get("u").asInstanceOf[Node].getProperty("username")
        val selection = context.actorSelection(s"akka://main/user/$username")
        selection ! Identify(username)
      }
    }
    case ActorIdentity(correlationId, actorRef) => {
      if (actorRef.isEmpty) {
        println(s"scanner for ${correlationId.toString} is not found so spawning")
        context.system.actorOf(UserScannerActor.props(correlationId.toString), correlationId.toString.replace(" ", "%20"))
      }
    }
  }
}
