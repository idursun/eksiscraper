package actors

import actors.RecommenderActor.Recommend
import akka.actor.{Props, ActorIdentity, Identify, Actor}
import db.{DbOperations, EmbeddedDatabaseService}
import org.neo4j.graphdb.Node
import scala.concurrent.duration._

object RecommenderActor {

  object Recommend

  def props = Props(new RecommenderActor)

}

class RecommenderActor extends Actor with EmbeddedDatabaseService with DbOperations {
  import db.ds
  import context.dispatcher

  self ! Recommend

  override def receive: Receive = {
    case Recommend => withTx {
      val results = database.execute("match (u:USER)-[:AUTHORED]->()<-[:FAVORITED]-(k:USER) with u, count(*) as c where c > 10 return u.username,c order by c desc")
      while (results.hasNext) {
        val row = results.next()
        val username = row.get("u.username").asInstanceOf[String]
        val selection = context.actorSelection(s"akka://main/user/${username.replace(" ", "%20")}")
        selection ! Identify(username)
      }
      context.system.scheduler.scheduleOnce(5.minutes, self, Recommend)
    }
    case ActorIdentity(correlationId, actorRef) =>
      if (actorRef.isEmpty) {
        println(s"scanner for ${correlationId.toString} is not found so spawning")
        context.system.actorOf(UserScannerActor.props(correlationId.toString), correlationId.toString.replace(" ", "%20"))
      }
  }
}
