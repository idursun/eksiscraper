import java.io.File

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory

package object db {
  def location = "db-data"
  implicit var ds: GraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(location))
}
