package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.testkit._

import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.test.{ImplicitTimeSource, TestActorSystem}
import io.quckoo.{JobSpec, Task, Trigger}

import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionDriverSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), TestArtifactId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

}

class ExecutionDriverSpec extends TestKit(TestActorSystem("ExecutionDriverSpec"))
    with ImplicitSender with ImplicitTimeSource
    with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers
    with Inside {

  import ExecutionDriver._
  import ExecutionDriverSpec._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe()

  before {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, eventListener.ref)
  }

  def executionDriverProps: Props = ExecutionDriver.props.
    withDispatcher("akka.actor.default-dispatcher")

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution driver with a recurring trigger that eventually gets disabled" should  {
    val trigger = Trigger.Every(50 millis)
    val executionProbe = TestProbe()
    val executionProps = TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(executionDriverProps,
      self, "executionDriverWithRecurringTrigger"
    )
    watch(executionPlan)

    "create an execution from a job specification" in {
      executionPlan ! New(TestJobId, TestJobSpec, planId, trigger, executionProps)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      within(50 millis) {
        executionProbe.expectMsgType[ExecutionLifecycle.Enqueue]
      }
    }

    "re-schedule an execution once it finishes" in {
      val successOutcome = Task.Success
      executionProbe.reply(ExecutionLifecycle.Result(successOutcome))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.planId shouldBe planId
      completedMsg.outcome shouldBe successOutcome

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId shouldBe TestJobId
      scheduledMsg.planId shouldBe planId

      within(100 millis) {
        executionProbe.expectMsgType[ExecutionLifecycle.Enqueue]
      }
    }

    "not re-schedule an execution after the job is disabled" in {
      mediator ! DistributedPubSubMediator.Publish(topics.Registry, JobDisabled(TestJobId))
      // Complete the execution so the driver can come back to ready state
      executionProbe.reply(ExecutionLifecycle.Result(Task.Success))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.outcome shouldBe Task.Success

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId shouldBe TestJobId
      finishedMsg.planId shouldBe planId

      executionProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }
  }

  "An execution driver with a recurring trigger that eventually gets cancelled" should {
    val trigger = Trigger.Every(50 millis)
    val executionProbe = TestProbe()
    val executionProps = TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionDriver = TestActorRef(executionDriverProps,
      self, "executionDriverWithRecurringTriggerAndCancellation"
    )
    watch(executionDriver)

    "create an execution from a job specification" in {
      executionDriver ! New(TestJobId, TestJobSpec, planId, trigger, executionProps)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      within(50 millis) {
        executionProbe.expectMsgType[ExecutionLifecycle.Enqueue]
      }
    }

    "instruct the execution to immediately stop when it's cancelled" in {
      executionDriver ! CancelExecutionPlan(planId)

      val cancelMsg = executionProbe.expectMsgType[ExecutionLifecycle.Cancel]
      cancelMsg.reason shouldBe Task.UserRequest

      executionProbe.reply(ExecutionLifecycle.Result(Task.Interrupted(Task.UserRequest)))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.outcome shouldBe Task.Interrupted(Task.UserRequest)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId shouldBe TestJobId
      finishedMsg.planId shouldBe planId

      executionProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionDriver ! PoisonPill
      expectTerminated(executionDriver)
    }
  }

  "An execution driver with non recurring trigger" should {
    val trigger = Trigger.Immediate
    val executionProbe = TestProbe()
    val executionProps = TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(executionDriverProps, self, "executionPlanWithOneShotTrigger")
    watch(executionPlan)

    "create an execution from a job specification" in {
      executionPlan ! New(TestJobId, TestJobSpec, planId, trigger, executionProps)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      executionProbe.expectMsgType[ExecutionLifecycle.Enqueue]
    }

    "terminate once the execution finishes" in {
      val successOutcome = Task.Success
      executionProbe.send(executionPlan, ExecutionLifecycle.Result(successOutcome))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId should be (TestJobId)
      completedMsg.planId should be (planId)
      completedMsg.outcome should be (successOutcome)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId should be (TestJobId)
      finishedMsg.planId should be (planId)

      executionProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }
  }

  "An execution plan that never gets fired" should {
    val trigger = Trigger.At(currentDateTime.minusHours(1), graceTime = Some(5 seconds))
    val executionProbe = TestProbe()
    val executionProps = TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(executionDriverProps, self, "executionPlanThatNeverFires")
    watch(executionPlan)

    "create an execution from a job specification and terminate it right after" in {
      executionPlan ! New(TestJobId, TestJobSpec, planId, trigger, executionProps)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId should be (TestJobId)
      finishedMsg.planId should be (planId)

      executionProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }
  }

}
