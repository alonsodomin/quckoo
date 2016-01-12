package io.kairos.worker

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit._
import io.kairos.cluster.Task
import io.kairos.id.{ArtifactId, TaskId}
import io.kairos.protocol.{ExceptionThrown, UnresolvedDependency}
import io.kairos.resolver.{Artifact, Resolver}
import org.scalatest._

import scala.concurrent.duration._
import scalaz._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JobExecutorSpec {
  val TestExecutionId: TaskId = UUID.randomUUID()
  val TestJobClass = "com.example.FooClass"
  val TestModuleId = ArtifactId("io.kairos", "test", "latest")
}

class JobExecutorSpec extends TestKit(ActorSystem("JobExecutorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with DefaultTimeout {

  import JobExecutorSpec._
  import Resolver._

  import Scalaz._

  val resolverProbe = TestProbe()
  val jobExecutor = TestActorRef(JobExecutor.props(resolverProbe.ref), self)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A job executor actor" must "fail an execution if the dependency resolution fails" in {
    val task = Task(TestExecutionId, artifactId = TestModuleId, jobClass = TestJobClass)
    val expectedResolutionFailed = UnresolvedDependency(TestModuleId)

    jobExecutor ! JobExecutor.Execute(task)

    resolverProbe.expectMsg(Acquire(TestModuleId))

    within(2 seconds) {
      resolverProbe.reply(expectedResolutionFailed.failureNel[Artifact])
      awaitAssert {
        expectMsg(JobExecutor.Failed(NonEmptyList(expectedResolutionFailed)))
      }
    }
  }

  it must "fail if instantiation of the job failed" in {
    val params = Map("a" -> 7)
    val task = Task(TestExecutionId, TestModuleId, params, TestJobClass)

    val expectedException = new ClassNotFoundException(TestJobClass)
    val failingPackage = Artifact(TestModuleId, Seq(new URL("http://www.example.com")))

    jobExecutor ! JobExecutor.Execute(task)

    resolverProbe.expectMsg(Acquire(TestModuleId))
    resolverProbe.reply(failingPackage)

    expectMsgType[JobExecutor.Failed].errors should be(ExceptionThrown(expectedException))
  }

}
