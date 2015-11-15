package db

import scala.collection.JavaConversions._

import org.neo4j.graphdb.{Relationship, Node}

import scala.util.{Failure, Success, Try}

trait DbOperations { self: EmbeddedDatabaseService =>

  def findUser(username: String): Option[Node] = Option(database.findNode(Labels.USER, "username", username))

  def createUser(username: String):Node = {
    val node = database.createNode(Labels.USER)
    node.setProperty("username", username)
    node
  }

  def isFavoritedBefore(username: String, entryId: String): Boolean = findEntry(entryId) match  {
    case Some(entry) =>
      val relationships = entry.getRelationships(RelTypes.FAVORITED).toList
      for(relationship <- relationships) {
        Try(relationship.getEndNode.getProperty("username")) match {
          case Success(value) if value == username => return true
        }
      }
      false
    case None => false
  }

  def markFavorited(user: Node, entryId: String) = {
    val entryNode = createEntryNode(entryId)
    user.createRelationshipTo(entryNode, RelTypes.FAVORITED)
    println(s"${user.getProperty("username")} favorited $entryId")
  }

  def findEntry(entryId: String): Option[Node] = {
    try {
      Option(db.ds.findNode(Labels.ENTRY, "id", entryId))
    } catch {
      case e: Exception => None
    }
  }

  def createEntryNode(entryId: String): Node = {
    println(s"creating entry node $entryId")
    val entryNode: Node = database.createNode(Labels.ENTRY)
    entryNode.setProperty("id", entryId)
    entryNode
  }

}
