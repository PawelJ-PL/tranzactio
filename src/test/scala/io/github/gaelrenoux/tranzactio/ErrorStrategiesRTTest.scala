package io.github.gaelrenoux.tranzactio

import java.util.{Properties, UUID}

import com.zaxxer.hikari.HikariDataSource
import zio._
import zio.blocking._
import zio.clock.Clock
import zio.duration._

//scalastyle:off
object ErrorStrategiesRTTest extends App {


  val source = {
    val ds = new HikariDataSource()
    ds.setJdbcUrl("jdbc:postgresql://localhost:54320/fire")
    ds.setUsername("fire")
    ds.setPassword("fire")
    ds.setMaximumPoolSize(1)
    ds.setConnectionTimeout(5000)

    val props = new Properties()
    ds.setDataSourceProperties(props)

    ds
  }

  source.getConnection()

//  def getConnection: RIO[Blocking, Unit] = effectBlocking {
//    log("Hello")
//    Thread.sleep(5000)
//    log("Goodbye")
//  }

  def getConnectionForReal: RIO[Blocking, Unit] = effectBlocking {
    log("Hello")
    source.getConnection
    log("Goodbye")
  }

  private def wrap[R, A](method: String, es: ErrorStrategy)(z: ZIO[Blocking, Throwable, A]) = es {
    z.mapError(e => DbException.Wrapped(e))
  }.tapError(
    _ => zlog(s"$method-total KO", error = true)
  )

  def openConnection: ZIO[Blocking with Clock, DbException, Unit] =
    wrap("_openConnection", ErrorStrategy.Nothing.withRetryTimeout(1.second)) {
      for {
        uuid <- ZIO.effectTotal(UUID.randomUUID().toString)
        _ <- zlog(s"_openConnection-$uuid start")
        result <- getConnectionForReal.tapError(e => zlog(s"_openConnection-$uuid KO: $e", error = true))
        _ <- zlog(s"_openConnection-$uuid OK: $result")
      } yield result
    }

  private def suspend: UIO[Unit] = ZIO.effectTotal(Thread.sleep(10000)).unit

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    (
      for {
        _ <- zlog("Started")
        _ <- openConnection.tapBoth(
          e => zlog(s"Failed: $e"),
          a => zlog(s"Succeeded: $a")
        )
      } yield ()
      ).either *> suspend.exitCode
  }
}
