package db

import org.neo4j.graphdb.GraphDatabaseService

trait EmbeddedDatabaseService {

  def database = db.ds

  def withTx(action: => Unit)(implicit db: GraphDatabaseService) = {
    val tx = db.beginTx
    try {
      action
      tx.success()
    } catch {
      case ex:Exception => tx.failure()
    } finally  {
      println("closing transaction")
      tx.close()
    }
  }

}
