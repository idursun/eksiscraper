package db

import scala.collection.JavaConversions._

import org.neo4j.graphdb.{Relationship, Node}

trait DbOperations { self: EmbeddedDatabaseService =>

  def findUser(username: String): Node = Option(db.findNode(Labels.USER, "username", username)) match {
    case Some(x)=> x
    case None =>
      val node = db.createNode(Labels.USER)
      node.setProperty("username", username)
      node
  }

  def markFavorited(user: Node, entryId: String): Boolean = {
    try {
      val node: Node = Option(db.findNode(Labels.ENTRY, "id", entryId)) getOrElse createEntryNode(entryId)
      val relationships = node.getRelationships(RelTypes.FAVORITED).toList
      for (x: Relationship <- relationships) {
        if (x.getEndNode.equals(user)) {
          false
        }
      }
      user.createRelationshipTo(node, RelTypes.FAVORITED)
      true
    } catch {
      case e: Exception => true
    }
  }

  def findEntry(entryId: String): Option[Node] = {
    try {
      Option(db.findNode(Labels.ENTRY, "id", entryId))
    } catch {
      case e: Exception => None
    }
  }

  def createEntryNode(entryId: String): Node = {
    println(s"creating entry node ${entryId}")
    val entryNode: Node = db.createNode(Labels.ENTRY)
    entryNode.setProperty("id", entryId)
    entryNode
  }

}
