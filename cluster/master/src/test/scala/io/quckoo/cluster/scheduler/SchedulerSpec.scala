package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor.Status
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.persistence.inmemory.query.scaladsl.InMemoryReadJournal
import akka.persistence.query.PersistenceQuery
import akka.testkit._

import io.quckoo.{ExecutionPlan, JobSpec, Task, Trigger}
import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId, PlanId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.test.{ForwardActorSubscriber, ImplicitTimeSource, TestActorSystem}

import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

  final val TestTrigger = Trigger.After(10 seconds)

}

//@Ignore
class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec"))
    with ImplicitSender with ImplicitTimeSource
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  import SchedulerSpec._
  import DistributedPubSubMediator._

  val registryProbe = TestProbe("registryProbe")
  val taskQueueProbe = TestProbe("taskQueueProbe")
  val shardProbe = TestProbe("shardRegionProbe")
  val indexProbe = TestProbe("indexProbe")

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

    val scheduler = TestActorRef(new Scheduler(
      readJournal,
      registryProbe.ref,
      shardProbe.ref,
      TestActors.forwardActorProps(taskQueueProbe.ref),
      ForwardActorSubscriber.props(indexProbe.ref)
    ), "scheduler")

    var testPlanId: Option[PlanId] = None

    "create an execution driver for an enabled job" in {
      scheduler ! ScheduleJob(TestJobId, trigger = TestTrigger)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(TestJobId -> TestJobSpec)

      val newDriverMsg = shardProbe.expectMsgType[ExecutionDriver.New]
      newDriverMsg.jobId shouldBe TestJobId
      newDriverMsg.spec shouldBe TestJobSpec
      newDriverMsg.trigger shouldBe TestTrigger

      shardProbe.send(mediator, Publish(topics.Scheduler, ExecutionPlanStarted(TestJobId, newDriverMsg.planId)))

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId shouldBe TestJobId
      startedMsg.planId shouldBe newDriverMsg.planId

      expectMsg(startedMsg)

      testPlanId = Some(startedMsg.planId)
    }

    "return the execution plan details when requested" in {
      testPlanId shouldBe defined

      testPlanId.foreach { planId =>
        scheduler ! GetExecutionPlan(planId)

        indexProbe.expectMsg(GetExecutionPlan(planId))
        indexProbe.reply(ExecutionPlan(TestJobId, planId, TestTrigger, currentDateTime))

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

        indexProbe.expectMsg(GetExecutionPlans)
        indexProbe.reply(ExecutionPlan(TestJobId, planId, TestTrigger, currentDateTime))
        indexProbe.reply(Status.Success(()))

        val executionPlans = expectMsgType[Map[PlanId, ExecutionPlan]]
        executionPlans should not be empty
        executionPlans should contain key planId
      }
    }

    "allow cancelling an execution plan" in {
      testPlanId shouldBe defined
      testPlanId foreach { planId =>
        scheduler ! CancelExecutionPlan(planId)

        shardProbe.expectMsg(CancelExecutionPlan(planId))
        shardProbe.send(mediator, Publish(topics.Scheduler, ExecutionPlanFinished(TestJobId, planId)))

        /*val completedMsg = eventListener.expectMsgType[TaskCompleted]
        completedMsg.planId shouldBe planId
        completedMsg.jobId shouldBe TestJobId*/

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
        indexProbe.expectMsg(GetExecutionPlans)
        indexProbe.reply(ExecutionPlan(TestJobId, planId, TestTrigger, currentDateTime,
          lastOutcome = Task.NeverRun(Task.UserRequest)
        ))
        indexProbe.reply(Status.Success(()))

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

      indexProbe.expectMsg(GetExecutionPlan(randomPlanId))
      indexProbe.reply(ExecutionPlanNotFound(randomPlanId))

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
