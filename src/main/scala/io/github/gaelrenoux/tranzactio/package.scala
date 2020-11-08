package io.github.gaelrenoux

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

import zio.{Has, UIO, ZIO}

package object tranzactio {

  type ConnectionSource = Has[ConnectionSource.Service]

  private val formatter = DateTimeFormatter.ofPattern("mm:ss.SSS").withZone(ZoneOffset.UTC)

  def log(str: String, error: Boolean = false): Unit = {
    val i = formatter.format(Instant.now())
    if (error) System.err.println(s"[$i] $str") else println(s"[$i] $str")
  }

  def zlog(str: String, error: Boolean = false): UIO[Unit] = ZIO.effectTotal(log(str, error))

  def zlogNewLine: UIO[Unit] = ZIO.effectTotal(println())

}
