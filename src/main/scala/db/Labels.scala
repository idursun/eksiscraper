package db

import org.neo4j.graphdb.Label

object Labels {

  val ENTRY : Label = new Label {
    override def name(): String = "ENTRY"
  }
  val USER: Label = new Label {
    override def name(): String = "USER"
  }

}
