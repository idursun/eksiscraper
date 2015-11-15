import actors.UserScannerActor
import actors.UserScannerActor.ScanPage
import akka.actor.ActorSystem
import db.{Labels, RelTypes, DbOperations, EmbeddedDatabaseService}
import org.neo4j.graphdb.Relationship

import scala.concurrent.Await
import scala.concurrent.duration._


case class ScrapedData(data: String)
case class EntryList(data: Array[String])

object Main extends App with EmbeddedDatabaseService with DbOperations {

//  withTx {
//    findUser("thex") match {
//      case Some(x) =>
//        println("found user")
//        val entryNode= findEntry("123") getOrElse createEntryNode("123")
//        val relations = x.getRelationships(RelTypes.FAVORITED)
//        import scala.collection.JavaConversions._
//
//        for(rel: Relationship <- relations) {
//          if (rel.getEndNode.equals(entryNode)) {
//            println("found relation")
//          }
//        }
//
////        x.createRelationshipTo(entryNode, RelTypes.FAVORITED)
////        println("created relation")
//
//      case None => val newUser = db.createNode(Labels.USER)
//        newUser.setProperty("username", "thex")
//        println("created user")
//    }
//  }

//  withTx {
//    val user = findUser("thex").get
//    val user2 = findUser("test1") getOrElse createUser("test1")
//    user.createRelationshipTo(user2, RelTypes.FAVORITED)
//    withTx {
//      user.createRelationshipTo(createUser("test4"), RelTypes.AUTHORED)
//    }
//    findUser("test4").get
//    println("founduser test4")
//  }

//  db.shutdown()

  lazy val system = ActorSystem("main")
  import system.dispatcher
  val actor = system.actorOf(UserScannerActor.props("thex"))

  system.scheduler.schedule(0.milliseconds, 5.minutes, actor, ScanPage(1))

  system.registerOnTermination({
    database.shutdown()
  })

  io.StdIn.readLine()
  println("shutting down system")
  Await.ready(system.terminate(), Duration.Inf)
//  println("shutting down db")
//  db.ds.shutdown()



}
