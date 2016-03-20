package io.kairos.cluster.scheduler

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.testkit._

import io.kairos.JobSpec
import io.kairos.id.{ArtifactId, JobId}
import io.kairos.cluster.registry.Registry
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.test.{ImplicitTimeSource, TestActorSystem}

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

}

class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec")) with ImplicitSender with ImplicitTimeSource
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  import SchedulerProtocol._
  import SchedulerSpec._

  val registryProbe = TestProbe("registry")
  val taskQueueProbe = TestProbe("taskQueue")

  val eventListener = TestProbe()
  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  before {
    mediator ! DistributedPubSubMediator.Subscribe(SchedulerTopic, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(SchedulerTopic, eventListener.ref)
  }

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A scheduler" should {
    val scheduler = TestActorRef(Scheduler.props(
      registryProbe.ref,
      TestActors.forwardActorProps(taskQueueProbe.ref)
    ), "scheduler")

    "create an execution driver for an enabled job" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
      registryProbe.reply(Some(TestJobSpec))

      eventListener.expectMsgType[ExecutionPlanStarted].jobId should be (TestJobId)
      eventListener.expectMsgType[TaskScheduled].jobId should be (TestJobId)

      expectMsgType[ExecutionPlanStarted].jobId should be (TestJobId)
    }

    "do nothing but reply if the job is not enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
      registryProbe.reply(Some(TestJobSpec.copy(disabled = true)))

      eventListener.expectNoMsg()
      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }

    "should reply not found if the job is not present" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
      registryProbe.reply(None)

      eventListener.expectNoMsg()
      expectMsgType[JobNotFound].jobId should be (TestJobId)
    }
  }

}
