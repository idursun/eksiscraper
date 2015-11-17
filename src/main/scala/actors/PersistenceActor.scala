package actors

import actors.PersistenceActor.PersistFavorited
import akka.actor.{Props, Actor}
import db.{RelTypes, Labels, EmbeddedDatabaseService, DbOperations}

object PersistenceActor {
  case class PersistFavorited(username: String, entryId: String)
  def props = Props(new PersistenceActor)
}

class PersistenceActor extends Actor with DbOperations with EmbeddedDatabaseService {
  import db.ds

  override def receive: Receive = {
    case PersistFavorited(username, entryId) => withTx {
//      val entryNode = findEntry(entryId) getOrElse createEntryNode(entryId)
      val userNode = findUser(username) getOrElse createUser(username)
      markFavorited(userNode, entryId)
//      userNode.createRelationshipTo(entryNode, RelTypes.FAVORITED)
    }
  }
}
