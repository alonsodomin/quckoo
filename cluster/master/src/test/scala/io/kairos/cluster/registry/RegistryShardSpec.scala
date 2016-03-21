package io.kairos.cluster.registry

import java.net.URL
import java.util.UUID

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit._

import io.kairos._
import io.kairos.fault.{ExceptionThrown, Fault, UnresolvedDependency}
import io.kairos.id.{ArtifactId, JobId}
import io.kairos.protocol.RegistryProtocol
import io.kairos.resolver.{Artifact, Resolve}
import io.kairos.test.TestActorSystem

import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scalaz._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistryShardSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val TestArtifact = Artifact(TestArtifactId, Seq(CommonsLoggingURL))

}

class RegistryShardSpec extends TestKit(TestActorSystem("RegistryShardSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll
    with Matchers with MockFactory {

  import RegistryProtocol._
  import RegistryShardSpec._
  import Scalaz._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe()
  var testJobId : Option[JobId] = None
  val mockResolve = mock[Resolve]

  before {
    mediator ! DistributedPubSubMediator.Subscribe(RegistryTopic, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(RegistryTopic, eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry shard" should {
    val registry = TestActorRef(RegistryShard.props(mockResolve).withDispatcher("akka.actor.default-dispatcher"))

    "return none when asked for a random job id" in {
      registry ! GetJob(JobId(UUID.randomUUID()))
      within(5 second) {
        awaitAssert(expectMsg(None))
      }
    }

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

    "return the registered job spec with its status when asked for it" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsg(Some(TestJobSpec))
      }
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      testJobId.foreach { id =>
        registry ! DisableJob(id)

        eventListener.expectMsgType[JobDisabled].jobId should be (id)
        expectMsgType[JobDisabled].jobId should be (id)
      }
    }

    "do nothing when trying to disable it again" in {
      testJobId.foreach { id =>
        registry ! DisableJob(id)

        eventListener.expectNoMsg()
        expectMsgType[JobDisabled].jobId should be (id)
      }
    }

    "return the registered job spec with disabled status" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsg(Some(TestJobSpec.copy(disabled = true)))
      }
    }

    "enable a job that has been previously disabled and publish the event" in {
      testJobId.foreach { id =>
        registry ! EnableJob(id)

        eventListener.expectMsgType[JobEnabled].jobId should be (id)
        expectMsgType[JobEnabled].jobId should be (id)
      }
    }

    "do nothing when trying to enable it again" in {
      testJobId.foreach { id =>
        registry ! EnableJob(id)

        eventListener.expectNoMsg()
        expectMsgType[JobEnabled].jobId should be (id)
      }
    }

    "double check that the job is finally enabled" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsg(Some(TestJobSpec))
      }
    }

  }

}
