package actors

import actors.EntryWorkerActor.{TrackFavorite, LinkEntry}
import akka.actor.{Actor, Props}
import db.{RelTypes, DbOperations, EmbeddedDatabaseService}

object EntryWorkerActor {

  case class LinkEntry(userName: String, entryId: String)

  case class TrackFavorite(userName: String, entryId: String)

  def props: Props = Props(new EntryWorkerActor)
}

class EntryWorkerActor extends Actor with EmbeddedDatabaseService with DbOperations {

  def receive = {

    case TrackFavorite(username, entryId) =>
      lazy val user = findUser(username)
      findEntry(entryId) match {
        case Some(x) =>
          user.createRelationshipTo(x, RelTypes.FAVORITED)
          println(s"creating relationship for ${entryId}")
        case None =>
          val entryNode = createEntryNode(entryId)
          user.createRelationshipTo(entryNode, RelTypes.AUTHORED)
          println(s"creating entry node ${entryId}")
      }

    case LinkEntry(username, entryId) =>
      withTx {
        val userNode = findUser(username)
        findEntry(entryId) match {
          case Some(x) => userNode.createRelationshipTo(x, RelTypes.AUTHORED)
          case None =>
            val entryNode = createEntryNode(entryId)
            userNode.createRelationshipTo(entryNode, RelTypes.AUTHORED)
        }
      }
  }
}
