package db

import java.io.File

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory

trait EmbeddedDatabaseService {

  def location = "db-data"
  var ds: Option[GraphDatabaseService] = None

  implicit def db = ds match {
    case Some(d) => d
    case None => ds = Some(new GraphDatabaseFactory().newEmbeddedDatabase(new File(location)))
      ds.get
  }

  def withTx(action: => Unit)(implicit db: GraphDatabaseService) = {
    val tx = db.beginTx
    try {
      action
      tx.success()
    } catch {
      case ex:Exception => tx.failure()
    }
  }

}
