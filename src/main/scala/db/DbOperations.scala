package db

import org.neo4j.graphdb.Node

import scala.collection.JavaConversions._

trait DbOperations {
  self: EmbeddedDatabaseService =>

  def findUser(username: String): Option[Node] = Option(database.findNode(Labels.USER, "username", username))

  def createUser(username: String): Node = {
    val node = database.createNode(Labels.USER)
    node.setProperty("username", username)
    node.setProperty("weight", 0)
    node
  }

  def isFavoritedBefore(userNode: Node, entryId: String): Boolean = {
    findEntry(entryId) match {
      case Some(entry) =>
        val relationships = entry.getRelationships(RelTypes.FAVORITED).toList
        relationships.exists(r => r.getStartNode.getId == userNode.getId)
      case None =>
        false
    }
  }

  def markFavorited(user: Node, entryId: String) = {
    val entryNode = findEntry(entryId) getOrElse createEntryNode(entryId)
    user.createRelationshipTo(entryNode, RelTypes.FAVORITED)
    println(s"${user.getProperty("username")} favorited $entryId")
  }

  def findEntry(entryId: String): Option[Node] = Option(database.findNode(Labels.ENTRY, "eid", entryId))

  def createEntryNode(entryId: String): Node = {
    println(s"creating entry node $entryId")
    val entryNode: Node = database.createNode(Labels.ENTRY)
    entryNode.setProperty("eid", entryId)
    entryNode.setProperty("p", 0)
    entryNode
  }

}
