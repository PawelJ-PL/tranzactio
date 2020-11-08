package io.github.gaelrenoux.tranzactio.issue18

import java.util.UUID

import io.github.gaelrenoux.tranzactio._
import zio.blocking.{Blocking, effectBlocking}
import zio.clock.Clock
import zio.duration._
import zio.{ExitCode, URIO, ZIO, App => ZioApp, _}

object ComplexBracketTest extends ZioApp {

  val timeout: Duration = 100.millis

  def getConnection: RIO[Blocking, MyConnection] = effectBlocking {
    Pool.getConnection
  }.uninterruptible

  def openConnection: ZIO[Any, DbException, MyConnection] = {
    for {
      uuid <- ZIO.effectTotal(UUID.randomUUID().toString)
      _ <- zlog(s"openConnection-$uuid start")
      result <- getConnection
        .tapBoth(
          e => zlog(s"openConnection-$uuid KO: $e", error = true),
          a => zlog(s"openConnection-$uuid OK: $a")
        ).uninterruptible.mapError(DbException.Wrapped)
    } yield result
  }
    .timeoutFail(DbException.Timeout(timeout))(timeout)
    .tapError(e => zlog(s"openConnection-global KO: $e", error = true))
    .provideLayer(Blocking.live ++ Clock.live)

  def closeConnection(c: MyConnection): ZIO[Any, DbException, Unit] = {
    for {
      uuid <- ZIO.effectTotal(UUID.randomUUID().toString)
      _ <- zlog(s"closeConnection-$uuid start")
      result <- effectBlocking(c.close())
        .tapError(e => zlog(s"closeConnection-$uuid KO: $e", error = true))
        .mapError(DbException.Wrapped)
      _ <- zlog(s"closeConnection-$uuid OK: $result")
    } yield result
  }
    .timeoutFail(DbException.Timeout(timeout))(timeout)
    .tapError(e => zlog(s"closeConnection-global KO: $e", error = true))
    .provideLayer(Blocking.live ++ Clock.live)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    val test1 = for {
      _ <- zlogNewLine
      _ <- zlog("One")
      _ <- openConnection.bracket(closeConnection(_).orDie) { _ => ZIO.succeed(()) }
      _ <- zlog("One ends")
    } yield ()

    val test2 = for {
      _ <- zlogNewLine
      _ <- zlog("Two")
      c <- openConnection
      _ <- closeConnection(c).either
      _ <- zlog("Two ends")
    } yield ()

    for {
      _ <- test1.tapError(e => zlog(s"Failed test1: $e", error = true)).either
      _ <- test2.tapError(e => zlog(s"Failed test2: $e", error = true)).either
    } yield ()
  }.exitCode

  val e = ErrorStrategies.all(ErrorStrategy(Schedule.recurs(5) && Schedule.spaced(1.second) , Duration.Infinity, Duration.Infinity))

}
