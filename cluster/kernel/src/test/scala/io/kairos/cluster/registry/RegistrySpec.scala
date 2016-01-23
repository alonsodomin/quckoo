package io.kairos.cluster.registry

import java.net.URL

import akka.testkit._
import io.kairos.JobSpec
import io.kairos.id.{ArtifactId, JobId}
import io.kairos.protocol.{Fault, RegistryProtocol, UnresolvedDependency}
import io.kairos.resolver.{Artifact, Resolver}
import io.kairos.test.TestActorSystem
import org.scalatest._

import scalaz._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistrySpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", "foo desc", TestArtifactId, "com.example.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val TestArtifact = Artifact(TestArtifactId, Seq(CommonsLoggingURL))

}

class RegistrySpec extends TestKit(TestActorSystem("RegistrySpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  import RegistryProtocol._
  import RegistrySpec._
  import Resolver._

  import Scalaz._

  val eventListener = TestProbe()
  var testJobId : Option[JobId] = None

  before {
    system.eventStream.subscribe(eventListener.ref, classOf[RegistryEvent])
  }

  after {
    system.eventStream.unsubscribe(eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry" should {
    val resolverProbe = TestProbe()
    val registry = TestActorRef(Registry.props(resolverProbe.ref))

    "reject a job if it fails to resolve its dependencies" in {
      val expectedResolutionFailed = UnresolvedDependency(TestArtifactId)

      registry ! RegisterJob(TestJobSpec)

      val resolveMsg = resolverProbe.expectMsgType[Validate]
      resolveMsg.artifactId should be (TestArtifactId)

      resolverProbe.reply(expectedResolutionFailed.failureNel[Artifact])

      expectMsgType[JobRejected].cause should be (NonEmptyList(expectedResolutionFailed))
    }

    "notify that a specific job is not enabled when attempting to disabling it" in {
      val otherModuleId = ArtifactId("com.example", "foo", "latest")
      val otherJobSpec = JobSpec("foo", "foo desc", otherModuleId, "com.example.Job")

      val nonExistentJobId = JobId(otherJobSpec)
      registry ! DisableJob(nonExistentJobId)

      expectMsgType[JobNotEnabled].jobId should be (nonExistentJobId)
    }

    "return job accepted if resolution of dependencies succeeds" in {
      registry ! RegisterJob(TestJobSpec)

      val resolveMsg = resolverProbe.expectMsgType[Validate]
      resolveMsg.artifactId should be (TestArtifactId)

      resolverProbe.reply(TestArtifact.successNel[Fault])

      val registryResponse = expectMsgType[JobAccepted]
      registryResponse.job should be (TestJobSpec)
      testJobId = Some(registryResponse.jobId)
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
