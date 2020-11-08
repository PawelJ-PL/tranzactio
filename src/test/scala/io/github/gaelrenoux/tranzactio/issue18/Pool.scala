package io.github.gaelrenoux.tranzactio.issue18

import java.time.Instant

object Pool {
  def getConnection: MyConnection = {
    println(s"${Instant.now} Looking for the connection")
    Thread.sleep(5000)
    println(s"${Instant.now} Connection borrowed")
    EmptyConnection
  }
}
