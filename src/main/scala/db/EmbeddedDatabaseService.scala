package db

import org.neo4j.graphdb.GraphDatabaseService

trait EmbeddedDatabaseService {

  implicit def database = db.ds

  def withTx(action: => Unit)(implicit db: GraphDatabaseService) = {
    val tx = db.beginTx
    try {
      action
      tx.success()
    } catch {
      case ex:Exception =>
        ex.printStackTrace()
        println(s"error: ${ex.toString}")
        tx.failure()
    } finally  {
      tx.close()
    }
  }

}
