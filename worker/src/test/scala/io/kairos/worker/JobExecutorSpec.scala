package io.kairos.worker

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit._
import io.kairos.cluster.Task
import io.kairos.id.{ModuleId, TaskId}
import io.kairos.protocol._
import io.kairos.resolver.{JobPackage, Resolver}
import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JobExecutorSpec {
  val TestExecutionId: TaskId = UUID.randomUUID()
  val TestJobClass = "com.example.FooClass"
  val TestModuleId = ModuleId("io.chronos", "test", "latest")
}

class JobExecutorSpec extends TestKit(ActorSystem("JobExecutorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with DefaultTimeout {

  import JobExecutorSpec._
  import Resolver._

  val resolverProbe = TestProbe()
  val jobExecutor = TestActorRef(JobExecutor.props(resolverProbe.ref), self)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A job executor actor" must "fail an execution if the dependency resolution fails" in {
    val task = Task(TestExecutionId, moduleId = TestModuleId, jobClass = TestJobClass)
    val expectedResolutionFailed = ResolutionFailed(Seq("com.bar.foo"))
    val expectedResolverResponse = Left(expectedResolutionFailed)

    jobExecutor ! JobExecutor.Execute(task)

    resolverProbe.expectMsg(Resolve(TestModuleId))

    within(2 seconds) {
      resolverProbe.reply(expectedResolutionFailed)
      awaitAssert {
        expectMsg(JobExecutor.Failed(expectedResolverResponse))
      }
    }
  }

  it must "fail if instantiation of the job failed" in {
    val params = Map("a" -> 7)
    val task = Task(TestExecutionId, TestModuleId, params, TestJobClass)

    val expectedException = new ClassNotFoundException(TestJobClass)
    val failingPackage = JobPackage(TestModuleId, Seq(new URL("http://www.example.com")))

    jobExecutor ! JobExecutor.Execute(task)

    resolverProbe.expectMsg(Resolve(TestModuleId))

    resolverProbe.reply(failingPackage)

    expectMsgType[JobExecutor.Failed].reason match {
      case Right(x) =>
        x.getClass should be (expectedException.getClass)
        x.getMessage should be (expectedException.getMessage)
      case _ =>
        fail("Expected a ClassNotFoundException as the cause of the failure.")
    }
  }

}
