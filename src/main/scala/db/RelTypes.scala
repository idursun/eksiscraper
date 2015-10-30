package db

import org.neo4j.graphdb.RelationshipType

object RelTypes {
  val FAVORITED: RelationshipType = new RelationshipType {
    override def name(): String = "FAVORITED"
  }

  val AUTHORED: RelationshipType = new RelationshipType {
    override def name(): String = "AUTHORED"
  }
}
