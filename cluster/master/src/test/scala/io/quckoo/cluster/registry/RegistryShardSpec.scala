package io.quckoo.cluster.registry

import java.net.URL
import java.util.UUID

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit._
import io.quckoo._
import io.quckoo.cluster.topics
import io.quckoo.fault.UnresolvedDependency
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.resolver.{Artifact, Resolve, Resolver}
import io.quckoo.test.TestActorSystem
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.duration._
import scalaz._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistryShardSpec {

  final val BarArtifactId = ArtifactId("com.example", "bar", "test")
  final val BarJobSpec = JobSpec("bar", Some("bar desc"), BarArtifactId, "com.example.bar.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val BarArtifact = Artifact(BarArtifactId, Seq(CommonsLoggingURL))

}

class RegistryShardSpec extends TestKit(TestActorSystem("RegistryShardSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll
    with Matchers with MockFactory {

  import RegistryShardSpec._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe()
  var testJobId : Option[JobId] = None
  val mockResolve = mock[Resolve]

  before {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Registry, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry shard" should {
    val resolverProbe = TestProbe()
    val registry = TestActorRef(RegistryShard.props(resolverProbe.ref).
      withDispatcher("akka.actor.default-dispatcher"))

    "return none when asked for a random job id" in {
      registry ! GetJob(JobId(UUID.randomUUID()))
      within(5 second) {
        awaitAssert(expectMsg(None))
      }
    }

    "reject a job if it fails to resolve its dependencies" in {
      val unresolvedDependency = UnresolvedDependency(BarArtifactId)

      registry ! RegisterJob(BarJobSpec)

      resolverProbe.expectMsg(Resolver.Validate(BarArtifactId))
      resolverProbe.reply(Resolver.ResolutionFailed(NonEmptyList(unresolvedDependency)))

      expectMsgType[JobRejected].cause should be (NonEmptyList(unresolvedDependency))
      eventListener.expectMsgType[JobRejected].cause should be (NonEmptyList(unresolvedDependency))
    }

    "return job accepted if resolution of dependencies succeeds" in {
      registry ! RegisterJob(BarJobSpec)

      resolverProbe.expectMsg(Resolver.Validate(BarArtifactId))
      resolverProbe.reply(Resolver.ArtifactResolved(BarArtifact))

      val registryResponse = expectMsgType[JobAccepted]
      registryResponse.job should be (BarJobSpec)

      testJobId = Some(registryResponse.jobId)
      eventListener.expectMsgType[JobAccepted].job should be (BarJobSpec)
    }

    "return the registered job spec with its status when asked for it" in {
      testJobId.foreach { id =>
        registry ! GetJob(id)

        expectMsg(Some(BarJobSpec))
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

        expectMsg(Some(BarJobSpec.copy(disabled = true)))
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

        expectMsg(Some(BarJobSpec))
      }
    }

  }

}
