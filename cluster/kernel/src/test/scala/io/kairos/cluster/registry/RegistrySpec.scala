package io.kairos.cluster.registry

import java.net.URL

import akka.testkit._
import io.kairos._
import io.kairos.id.{ArtifactId, JobId}
import io.kairos.protocol.RegistryProtocol
import io.kairos.resolver.{Artifact, Resolve}
import io.kairos.test.TestActorSystem
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistrySpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val TestArtifact = Artifact(TestArtifactId, Seq(CommonsLoggingURL))

}

class RegistrySpec extends TestKit(TestActorSystem("RegistrySpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers with MockFactory {

  import RegistryProtocol._
  import RegistrySpec._

  import Scalaz._

  val eventListener = TestProbe()
  var testJobId : Option[JobId] = None

  val mockResolve = mock[Resolve]

  before {
    system.eventStream.subscribe(eventListener.ref, classOf[RegistryEvent])
  }

  after {
    system.eventStream.unsubscribe(eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry" should {
    val registry = TestActorRef(Registry.props(mockResolve))

    "reject a job if it fails to resolve its dependencies" in {
      val unresolvedDependency = UnresolvedDependency(TestArtifactId)

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(TestArtifactId, false, *).
        returning(Future.successful(unresolvedDependency.failureNel[Artifact]))

      registry ! RegisterJob(TestJobSpec)

      expectMsgType[JobRejected].cause should be (NonEmptyList(unresolvedDependency))
      eventListener.expectMsgType[JobRejected].cause should be (NonEmptyList(unresolvedDependency))
    }

    "reject a job if its resolution throws an unexpected exception" in {
      val expectedException = new Exception("TEST EXCEPTION")

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(TestArtifactId, false, *).
        returning(Future.failed[Validated[Artifact]](expectedException))

      registry ! RegisterJob(TestJobSpec)

      expectMsgType[JobRejected].cause should be (NonEmptyList(ExceptionThrown(expectedException)))
      eventListener.expectMsgType[JobRejected].cause should be (NonEmptyList(ExceptionThrown(expectedException)))
    }

    "notify that a specific job is not enabled when attempting to disabling it" in {
      val otherModuleId = ArtifactId("com.example", "foo", "latest")
      val otherJobSpec = JobSpec("foo", "foo desc", otherModuleId, "com.example.Job")

      val nonExistentJobId = JobId(otherJobSpec)
      registry ! DisableJob(nonExistentJobId)

      expectMsgType[JobNotEnabled].jobId should be (nonExistentJobId)
    }

    "return job accepted if resolution of dependencies succeeds" in {
      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(TestArtifactId, false, *).
        returning(Future.successful(TestArtifact.successNel[Fault]))

      registry ! RegisterJob(TestJobSpec)

      val registryResponse = expectMsgType[JobAccepted]
      registryResponse.job should be (TestJobSpec)
      testJobId = Some(registryResponse.jobId)

      eventListener.expectMsgType[JobAccepted].job should be (TestJobSpec)
    }

    "return the registered job spec when asked for it" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsg(TestJobSpec)
      }
    }

    "return a collection of all the registered jobs when asked for it" in {
      registry ! GetJobs

      val returnedSeq = expectMsgType[Seq[JobSpec]]
      returnedSeq should contain (TestJobSpec)
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      testJobId.foreach { id =>
        registry ! DisableJob(id)

        expectMsgType[JobDisabled].jobId should be (id)
        eventListener.expectMsgType[JobDisabled].jobId should be (id)
      }
    }

    "return a job not enabled message when asked for a job after disabling it" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsgType[JobNotEnabled].jobId should be (id)
      }
    }

    "return an empty collection when there are not enabled jobs" in {
      registry ! GetJobs

      val returnedSeq = expectMsgType[Seq[JobSpec]]
      returnedSeq should be (empty)
    }
  }

}
