package actors

import actors.PersistenceActor.PersistFavorited
import akka.actor.{Actor, ActorLogging, Props}
import db.{RelTypes, DbOperations, EmbeddedDatabaseService}

object PersistenceActor {

  case class PersistFavorited(username: String, entryId: String)

  def props = Props(new PersistenceActor)
}

class PersistenceActor extends Actor with DbOperations with EmbeddedDatabaseService with ActorLogging {

  override def receive: Receive = {
    case PersistFavorited(username, entryId) => withTx {
      val userNode = findUser(username) getOrElse createUser(username)
      val entryNode = findEntry(entryId) getOrElse createEntryNode(entryId)
      userNode.createRelationshipTo(entryNode, RelTypes.FAVORITED)
      log.debug(s"${userNode.getProperty("username")} favorited $entryId")
    }
  }
}
