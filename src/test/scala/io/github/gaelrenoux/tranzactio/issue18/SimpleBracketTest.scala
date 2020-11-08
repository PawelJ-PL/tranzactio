package io.github.gaelrenoux.tranzactio.issue18

import java.time.Instant

import zio.blocking.effectBlocking
import zio.{App, ExitCode, URIO, ZIO}
import zio.duration._

//scalastyle:off
object SimpleBracketTest extends App {

  val timeout: Duration = 100.millis

  def openConnection = effectBlocking(Pool.getConnection)
    .timeoutFail(new Exception("Timeout"))(100.millis)

  def closeConnection(c: MyConnection) = effectBlocking(c.close()).orDie

  def doSomething(c: MyConnection) = effectBlocking(println("I did something!"))

  val z = openConnection.bracket(closeConnection) { c =>
    doSomething(c)
  }

  val prog = z.timeoutFail(new Exception("Timeout"))(100.millis).tapError(e => ZIO.effectTotal(e.printStackTrace()))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    prog.either.exitCode
  }
}
