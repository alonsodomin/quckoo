package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.persistence.inmemory.query.scaladsl.InMemoryReadJournal
import akka.persistence.query.PersistenceQuery
import akka.testkit._

import io.quckoo.{ExecutionPlan, JobSpec, Task, Trigger}
import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId, PlanId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.test.{ImplicitTimeSource, TestActorSystem}

import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

}

//@Ignore
class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec"))
    with ImplicitSender with ImplicitTimeSource
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
    ).withDispatcher("akka.actor.default-dispatcher"), "scheduler")
    var testPlanId: Option[PlanId] = None

    "create an execution driver for an enabled job" in {
      scheduler ! ScheduleJob(TestJobId, trigger = Trigger.After(10 seconds))

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(TestJobId -> TestJobSpec)

      eventListener.expectMsgType[ExecutionPlanStarted].jobId shouldBe TestJobId

      val startedMsg = expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId shouldBe TestJobId

      eventListener.expectMsgType[TaskScheduled].jobId shouldBe TestJobId

      testPlanId = Some(startedMsg.planId)
    }

    "return the execution plan details when requested" in {
      testPlanId shouldBe defined

      testPlanId.foreach { planId =>
        scheduler ! GetExecutionPlan(planId)

        val executionPlan = expectMsgType[ExecutionPlan]

        executionPlan.jobId shouldBe TestJobId
        executionPlan.planId shouldBe planId
        executionPlan.finished shouldBe false
      }
    }

    "return a map containing the current live execution plan" in {
      testPlanId shouldBe defined
      testPlanId.foreach { planId =>
        scheduler ! GetExecutionPlans

        val executionPlans = expectMsgType[Map[PlanId, ExecutionPlan]]
        executionPlans should not be empty
        executionPlans should contain key planId
      }
    }

    "allow cancelling an execution plan" in {
      testPlanId shouldBe defined
      testPlanId foreach { planId =>
        scheduler ! CancelExecutionPlan(planId)

        val completedMsg = eventListener.expectMsgType[TaskCompleted]
        completedMsg.planId shouldBe planId
        completedMsg.jobId shouldBe TestJobId

        val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
        finishedMsg.planId shouldBe planId
        finishedMsg.jobId shouldBe TestJobId

        expectMsg(finishedMsg)
      }
    }

    "return an map of finished execution plans when there is none active" in {
      scheduler ! GetExecutionPlans

      testPlanId shouldBe defined
      testPlanId foreach { planId =>
        val plans = expectMsgType[Map[PlanId, ExecutionPlan]]

        plans should contain key planId
        plans(planId) should matchPattern {
          case ExecutionPlan(`TestJobId`, `planId`, _, _, _, Task.NeverRun(Task.UserRequest), _, _, _, _) =>
        }
      }
    }

    "return execution plan not found when asked for a non-existent plan" in {
      val randomPlanId = UUID.randomUUID()

      scheduler ! GetExecutionPlan(randomPlanId)

      expectMsg(ExecutionPlanNotFound(randomPlanId))
    }

    "do nothing but reply if the job is not enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(TestJobId -> TestJobSpec.copy(disabled = true))

      eventListener.expectNoMsg()
      expectMsgType[JobNotEnabled].jobId shouldBe TestJobId
    }

    "should reply not found if the job is not present" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(JobNotFound(TestJobId))

      eventListener.expectNoMsg()
      expectMsgType[JobNotFound].jobId shouldBe TestJobId
    }
  }

}
