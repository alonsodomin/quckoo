package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor.Props
import akka.testkit._

import io.quckoo.Task
import io.quckoo.cluster.scheduler.TaskQueue.EnqueueAck
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id._
import io.quckoo.test.TestActorSystem

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by domingueza on 18/08/15.
 */
object ExecutionSpec {

  final val TestArtifactId = ArtifactId("com.example", "example", "test")
  final val TestJobClass = "com.example.Job"

}

class ExecutionSpec extends TestKit(TestActorSystem("ExecutionSpec"))
    with ImplicitSender with DefaultTimeout with WordSpecLike
    with BeforeAndAfterAll with Matchers {

  import Execution._
  import ExecutionSpec._
  import Task._

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  // We need to use a multi-threaded dispatcher to be able to realiably test the FSM
  private def executionProps(planId: PlanId,
                             enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
                             maxEnqueueAttempts: Int = DefaultMaxEnqueueAttempts,
                             executionTimeout: Option[FiniteDuration] = None): Props =
    Execution.props(planId, enqueueTimeout, maxEnqueueAttempts, executionTimeout).
      withDispatcher("akka.actor.default-dispatcher")

  "An execution cancelled before enqueuing" should {
    val planId = UUID.randomUUID()

    val enqueueTimeout = 2 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "non-enqueued-exec")
    watch(execution)

    "terminate when receiving the Cancel signal" in {
      execution.underlyingActor.stateName should be (Scheduled)

      execution ! Get

      val state = expectMsgType[ExecutionState]
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! Cancel(UserRequest)

      val resultMsg = expectMsgType[Result]
      resultMsg.outcome should be (NeverRun(UserRequest))

      expectTerminated(execution)
    }
  }

  "An execution that fails to enqueue" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-1")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 2 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "enqueued-timedout-exec")
    watch(execution)

    "request to enqueue after receiving WakeUp signal" in {
      execution.underlyingActor.stateName should be (Scheduled)

      execution ! Get

      val state = expectMsgType[ExecutionState]
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)

      execution.underlyingActor.stateName should be (Scheduled)
    }

    "retry to enqueue after timeout and then give up" in {
      val maxToWait = enqueueTimeout + (2 seconds)

      val enqueueMsg = within(enqueueTimeout, maxToWait) {
        taskQueue.expectMsgType[TaskQueue.Enqueue]
      }
      enqueueMsg.task should be (task)

      val resultMsg = within(enqueueTimeout, maxToWait) {
        expectMsgType[Result]
      }
      resultMsg.outcome should matchPattern {
        case NeverRun(_) =>
      }

      expectTerminated(execution)
    }
  }

  "An execution that is cancelled before starting" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-2")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "cancelled-before-starting-exec")
    watch(execution)

    "move to Waiting state after receiving an enqueue ack" in {
      execution.underlyingActor.stateName should be (Scheduled)

      val state = within(100 millis) {
        execution ! Get
        expectMsgType[ExecutionState]
      }
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(execution.underlyingActor.stateName should be (Waiting))
      }

      expectMsg(Triggered)
    }

    "terminate when requested to be cancelled" in {
      execution ! Cancel(UserRequest)

      within(2 seconds) {
        val resultMsg = expectMsgType[Result]
        resultMsg.outcome should be (NeverRun(UserRequest))
      }
      expectTerminated(execution)
    }
  }

  "An execution in progress" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-3")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "cancelled-while-in-progress-exec")
    watch(execution)

    "move to Waiting state after receiving an enqueue ack" in {
      execution.underlyingActor.stateName should be (Scheduled)

      val state = within(100 millis) {
        execution ! Get
        expectMsgType[ExecutionState]
      }
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(execution.underlyingActor.stateName should be (Waiting))
      }

      expectMsg(Triggered)
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(execution, Start)
      awaitAssert(execution.underlyingActor.stateName should be (InProgress))
    }

    "send Interrupted as result when requested to Cancel" in {
      execution ! Cancel(UserRequest)

      within(2 seconds) {
        val resultMsg = expectMsgType[Result]
        resultMsg.outcome should be (Interrupted(UserRequest))
      }
      expectTerminated(execution)
    }
  }

  "An execution that times out while running" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-4")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val executionTimeout = 1 second
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2, Some(executionTimeout)
    ), self, "timed-out-while-in-progress-exec")
    watch(execution)

    "move to Waiting state after receiving an enqueue ack" in {
      execution.underlyingActor.stateName should be (Scheduled)

      val state = within(100 millis) {
        execution ! Get
        expectMsgType[ExecutionState]
      }
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(execution.underlyingActor.stateName should be (Waiting))
      }

      expectMsg(Triggered)
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(execution, Start)
      awaitAssert(execution.underlyingActor.stateName should be (InProgress))
    }

    "request to the queue to time out the task" in {
      val timeoutMsg = within(executionTimeout + (1 second)) {
        taskQueue.expectMsgType[TaskQueue.TimeOut]
      }
      timeoutMsg.taskId should be (task.id)
    }

    "send NeverEnding as result when the queue replies" in {
      taskQueue.reply(TimeOut)

      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }
      resultMsg.outcome should be (NeverEnding)
      expectTerminated(execution)
    }
  }

  "An execution that fails while running" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-5")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "fails-while-in-progress-exec")
    watch(execution)

    "move to Waiting state after receiving an enqueue ack" in {
      execution.underlyingActor.stateName should be (Scheduled)

      val state = within(100 millis) {
        execution ! Get
        expectMsgType[ExecutionState]
      }
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(execution.underlyingActor.stateName should be (Waiting))
      }

      expectMsg(Triggered)
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(execution, Start)
      awaitAssert(execution.underlyingActor.stateName should be (InProgress))
    }

    "reply with the fault that caused the failure" in {
      val fault = ExceptionThrown.from(new RuntimeException("TEST EXCEPTION"))

      taskQueue.send(execution, Finish(Some(fault)))
      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }

      resultMsg.outcome should be (Task.Failure(fault))
      expectTerminated(execution)
    }
  }

  "An execution that completes successfully" should {
    val planId = UUID.randomUUID()
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestProbe("queue-6")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val execution = TestActorRef[Execution](executionProps(
      planId, enqueueTimeout, maxEnqueueAttempts = 2
    ), self, "successful-exec")
    watch(execution)

    "move to Waiting state after receiving an enqueue ack" in {
      execution.underlyingActor.stateName should be (Scheduled)

      val state = within(100 millis) {
        execution ! Get
        expectMsgType[ExecutionState]
      }
      state.planId should be (planId)
      state.task should be (None)
      state.queue should be (None)
      state.outcome should be (NotStarted)

      execution ! WakeUp(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(execution.underlyingActor.stateName should be (Waiting))
      }

      expectMsg(Triggered)
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(execution, Start)
      awaitAssert(execution.underlyingActor.stateName should be (InProgress))
    }

    "reply Success when it completes" in {
      taskQueue.send(execution, Finish(None))
      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }

      resultMsg.outcome should be (Task.Success)
      expectTerminated(execution)
    }
  }

}
