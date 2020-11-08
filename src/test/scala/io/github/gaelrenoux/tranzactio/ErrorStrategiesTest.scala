package io.github.gaelrenoux.tranzactio


import zio._
import zio.duration._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

class ErrorStrategiesTest extends RunnableSpec[TestEnvironment, Any] {

  type Env = TestEnvironment
  type Spec = ZSpec[Env, Any]

  //implicit private val errorStrategies: ErrorStrategies = ErrorStrategies.Nothing

  override def aspects: List[TestAspect[Nothing, Env, Nothing, Any]] = List(TestAspect.timeout(60.seconds))

  override def runner: TestRunner[Env, Any] = TestRunner(TestExecutor.default(testEnvironment))

  def spec: Spec = suite("ErrorStrategies Retry Tests")(
    testBasicRetry
  )

  private val testBasicRetry = testM("basic retry") {

    def runTest(trace: Ref[List[String]]) = {
      trace.update("ah" :: _) *> ZIO.effectTotal(42)
    }

    for {
      trace <- Ref.make[List[String]](Nil)
      forked <- runTest(trace).fork
      _ <- TestClock.adjust(1.second).repeatWhileM(_ => forked.status.map(!_.isDone))
      _ <- forked.join
      result <- trace.get
    } yield assert(result)(equalTo("end" :: "start" :: "end" :: "start" :: Nil))
  }

}
