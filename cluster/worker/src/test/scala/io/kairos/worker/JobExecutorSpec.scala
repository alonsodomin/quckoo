package io.kairos.worker

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit._
import io.kairos.cluster.Task
import io.kairos.id.{ArtifactId, TaskId}
import io.kairos.resolver.{Artifact, Resolve}
import io.kairos.{ExceptionThrown, Fault, UnresolvedDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JobExecutorSpec {

  final val TestExecutionId: TaskId = UUID.randomUUID()
  final val TestJobClass = "com.example.FooClass"
  final val TestArtifactId = ArtifactId("io.kairos", "test", "latest")

}

class JobExecutorSpec extends TestKit(ActorSystem("JobExecutorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with DefaultTimeout with MockFactory {

  import JobExecutorSpec._

  import Scalaz._

  val mockResolve = mock[Resolve]
  val jobExecutor = TestActorRef(JobExecutor.props(mockResolve), self)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A job executor actor" must "fail an execution if the dependency resolution fails" in {
    val task = Task(TestExecutionId, artifactId = TestArtifactId, jobClass = TestJobClass)
    val unresolvedDependency = UnresolvedDependency(TestArtifactId)

    (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
      expects(TestArtifactId, true, *).
      returning(Future.successful(unresolvedDependency.failureNel[Artifact]))

    jobExecutor ! JobExecutor.Execute(task)

    expectMsg(JobExecutor.Failed(NonEmptyList(unresolvedDependency)))
  }

  it must "fail if instantiation of the job failed" in {
    val params = Map("a" -> 7)
    val task = Task(TestExecutionId, TestArtifactId, params, TestJobClass)

    val expectedException = new ClassNotFoundException(TestJobClass)
    val failingPackage = Artifact(TestArtifactId, Seq(new URL("http://www.example.com")))

    (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
      expects(TestArtifactId, true, *).
      returning(Future.successful(failingPackage.successNel[Fault]))

    jobExecutor ! JobExecutor.Execute(task)

    expectMsgType[JobExecutor.Failed].errors should be(NonEmptyList(ExceptionThrown(expectedException)))
  }

}
