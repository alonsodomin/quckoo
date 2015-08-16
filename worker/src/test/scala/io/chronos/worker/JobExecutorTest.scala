package io.chronos.worker

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import io.chronos.cluster.Work
import io.chronos.id.{ExecutionId, ModuleId}
import io.chronos.protocol._
import io.chronos.resolver.{JobPackage, ModuleResolver}
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JobExecutorTest {
  val TestExecutionId: ExecutionId = ((UUID.randomUUID(), 1), 1)
  val TestJobClass = "com.example.FooClass"
  val TestModuleId = ModuleId("io.chronos", "test", "latest")
}

class JobExecutorTest extends TestKit(ActorSystem("JobExecutorTest")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with MockFactory with DefaultTimeout with ScalaFutures {

  import JobExecutorTest._

  val mockModuleResolver = mock[ModuleResolver]
  val jobExecutor = TestActorRef(JobExecutor.props(mockModuleResolver))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A job executor actor" must "fail an execution if the dependency resolution fails" in {
    val work = Work(TestExecutionId, moduleId = TestModuleId, jobClass = TestJobClass)
    val expectedResolutionFailed = ResolutionFailed(Seq("com.bar.foo"))

    (mockModuleResolver.resolve _).expects(TestModuleId, true).returning(Left(expectedResolutionFailed))

    jobExecutor ! JobExecutor.Execute(work)

    expectMsg(JobExecutor.Failed(TestExecutionId, Left(expectedResolutionFailed)))
  }

  it must "fail if instantiation of the job failed" in {
    val params = Map("a" -> 7)
    val work = Work(TestExecutionId, params, TestModuleId, TestJobClass)

    val expectedException = new ClassNotFoundException(TestJobClass)
    val failingPackage = JobPackage(TestModuleId, Seq(new URL("http://www.example.com")))

    (mockModuleResolver.resolve _).expects(TestModuleId, true).returning(Right(failingPackage))

    val result = (jobExecutor ? JobExecutor.Execute(work)).mapTo[JobExecutor.Failed]

    whenReady(result) { res =>
      res.executionId should be (TestExecutionId)
      res.reason match {
        case Right(x) =>
          x.getClass should be (expectedException.getClass)
          x.getMessage should be (expectedException.getMessage)
        case _ =>
          fail("Expected a ClassNotFoundException as the cause of the failure.")
      }
    }
  }

}
