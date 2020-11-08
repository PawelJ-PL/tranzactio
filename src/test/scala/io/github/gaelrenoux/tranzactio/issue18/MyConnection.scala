package io.github.gaelrenoux.tranzactio.issue18

import java.sql.Connection

trait MyConnection {
  def close() = println("Connection released")
}

case class WrappingConnection(c: Connection) extends MyConnection {
  override def close(): Unit = c.close()
}

object EmptyConnection extends MyConnection
