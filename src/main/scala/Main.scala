import java.util

import actors.UserScannerActor
import actors.UserScannerActor.ScanUser
import akka.actor.ActorSystem
import db.EmbeddedDatabaseService
import scala.concurrent.duration._


case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Main extends App with EmbeddedDatabaseService {

//  withTx {
//    val q = db.execute("MATCH (n: {'username': 'ssg'})")
//    while(q.hasNext) {
//      val next: util.Map[String, AnyRef] = q.next()
//      println(s"next ${next}")
//    }
//  }

//  withTx({
//    var node = db.findNode(Labels.USER, "username", "ssg")
//    if (node == null) {
//      node = db.createNode(Labels.USER)
//      node.setProperty("username", "ssg")
//    }
//
//    val node2: Node = db.createNode(Labels.ENTRY)
//    node.createRelationshipTo(node2, RelTypes.FAVORITED)
//  })
//
//  withTx({
//    val ssg = findUser("ssg")
//    val degree = ssg.getDegree(Direction.OUTGOING)
//    val rels = ssg.getRelationships(RelTypes.FAVORITED)
//    rels.forEach(new Consumer[Relationship] {
//      override def accept(t: Relationship): Unit = println(t.getEndNode.getId)
//    })
//
//    ssg.getLabels.forEach(new Consumer[Label] {
//      override def accept(t: Label): Unit = print(t.name())
//    })
//
//    println(rels)
//  })
//
//  db.shutdown()



  lazy val system = ActorSystem("main")
  import system.dispatcher

  val actor = system.actorOf(UserScannerActor.props)

  system.scheduler.schedule(0.milliseconds, 5.minutes, actor, ScanUser("thex"))

  system.registerOnTermination({
    db.shutdown()
  })

  system.awaitTermination()
}
