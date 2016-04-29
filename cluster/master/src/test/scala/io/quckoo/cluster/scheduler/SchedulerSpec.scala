package io.quckoo.cluster.scheduler

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.persistence.inmemory.query.journal.scaladsl.InMemoryReadJournal
import akka.persistence.query.PersistenceQuery
import akka.testkit._

import io.quckoo.JobSpec
import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.test.{ImplicitTimeSource, TestActorSystem}

import org.scalatest._

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
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, eventListener.ref)
  }

  val readJournal = PersistenceQuery(system).
    readJournalFor[InMemoryReadJournal](InMemoryReadJournal.Identifier)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A scheduler" should {
    val scheduler = TestActorRef(Scheduler.props(
      registryProbe.ref, readJournal,
      TestActors.forwardActorProps(taskQueueProbe.ref)
    ), "scheduler")

    "create an execution driver for an enabled job" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId should be (TestJobId)
      registryProbe.reply(TestJobId -> TestJobSpec)

      eventListener.expectMsgType[ExecutionPlanStarted].jobId should be (TestJobId)
      eventListener.expectMsgType[TaskScheduled].jobId should be (TestJobId)

      expectMsgType[ExecutionPlanStarted].jobId should be (TestJobId)
    }

    "do nothing but reply if the job is not enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId should be (TestJobId)
      registryProbe.reply(TestJobId -> TestJobSpec.copy(disabled = true))

      eventListener.expectNoMsg()
      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }

    "should reply not found if the job is not present" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId should be (TestJobId)
      registryProbe.reply(JobNotFound(TestJobId))

      eventListener.expectNoMsg()
      expectMsgType[JobNotFound].jobId should be (TestJobId)
    }
  }

}
