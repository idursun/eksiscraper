package db

import org.neo4j.graphdb.Node

trait DbOperations { self: EmbeddedDatabaseService =>

  def findUser(username: String): Node = Option(db.findNode(Labels.USER, "username", username)) match {
    case Some(x)=> x
    case None =>
      val node = db.createNode(Labels.USER)
      node.setProperty("username", username)
      node
  }

  def isProcessed(username: String, entryId: String): Option[Node] = {
    try {
      val user = findUser(username)
      val node: Node = db.findNode(Labels.ENTRY, "id", entryId)
      node.getRelationships(RelTypes.FAVORITED)

    } catch {
      case e: Exception => None
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
    val entryNode: Node = db.createNode(Labels.ENTRY)
    entryNode.setProperty("id", entryId)
    entryNode
  }

}
