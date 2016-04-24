package io.quckoo.cluster.registry

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit._

import io.quckoo._
import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.test.TestActorSystem

import org.scalatest._

/**
 * Created by domingueza on 21/08/15.
 */
object JobStateSpec {

  final val BarArtifactId = ArtifactId("com.example", "bar", "test")
  final val BarJobSpec    = JobSpec("bar", Some("bar desc"), BarArtifactId, "com.example.bar.Job")
  final val BarJobId      = JobId(BarJobSpec)

}

class JobStateSpec extends TestKit(TestActorSystem("JobStateSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll
    with Matchers {

  import JobStateSpec._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe()

  before {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Registry, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job state actor" should {
    val jobState = TestActorRef(JobState.props)

    "return job accepted when receiving a create command" in {
      jobState ! JobState.CreateJob(BarJobId, BarJobSpec)

      val stateResponse = expectMsgType[JobAccepted]
      stateResponse.job should be (BarJobSpec)

      eventListener.expectMsgType[JobAccepted].job should be (BarJobSpec)
    }

    "return the registered job spec with its status when asked for it" in {
      jobState ! GetJob(BarJobId)
      expectMsg(BarJobId -> BarJobSpec)
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      jobState ! DisableJob(BarJobId)

      eventListener.expectMsgType[JobDisabled].jobId should be (BarJobId)
      expectMsgType[JobDisabled].jobId should be (BarJobId)
    }

    "do nothing when trying to disable it again" in {
      jobState ! DisableJob(BarJobId)

      eventListener.expectNoMsg()
      expectMsgType[JobDisabled].jobId should be (BarJobId)
    }

    "return the registered job spec with disabled status" in {
      jobState ! GetJob(BarJobId)

      expectMsg(BarJobId -> BarJobSpec.copy(disabled = true))
    }

    "enable a job that has been previously disabled and publish the event" in {
      jobState ! EnableJob(BarJobId)

      eventListener.expectMsgType[JobEnabled].jobId should be (BarJobId)
      expectMsgType[JobEnabled].jobId should be (BarJobId)
    }

    "do nothing when trying to enable it again" in {
      jobState ! EnableJob(BarJobId)

      eventListener.expectNoMsg()
      expectMsgType[JobEnabled].jobId should be (BarJobId)
    }

    "double check that the job is finally enabled" in {
      jobState ! GetJob(BarJobId)

      expectMsg(BarJobId -> BarJobSpec)
    }

  }

}