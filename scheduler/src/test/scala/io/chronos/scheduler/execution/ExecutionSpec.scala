package io.chronos.scheduler.execution

import java.util.UUID

import akka.pattern._
import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import io.chronos.scheduler.queue.TaskQueue
import io.chronos.scheduler.queue.TaskQueue.EnqueueAck
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by domingueza on 18/08/15.
 */
object ExecutionSpec {

  val TestModuleId = ModuleId("com.example", "example", "test")
  val TestJobClass = "com.example.Job"

}

class ExecutionSpec extends TestKit(TestActorSystem("ExecutionSpec")) with ImplicitSender with DefaultTimeout
  with WordSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {

  import Execution._
  import ExecutionSpec._

  val planId = UUID.randomUUID()
  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A full running execution" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      self, "FullPathExecution"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      execution ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      within(2 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[Execution].stateName should be (Waiting)
        }
      }
    }

    "return an empty outcome again" in {
      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become in progress when notified to start" in {
      execution ! Start

      awaitAssert({
        execution.underlying.actor.asInstanceOf[Execution].stateName should be (InProgress)
      }, 2 seconds)
    }

    "send result to parent when is finished" in {
      val result: Int = 8392
      execution ! Finish(Right(result))

      expectMsg(Success(result))
      expectTerminated(execution)
    }
  }

  "An execution cancelled while sleeping" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      self, "SleepingExecution"
    )
    watch(execution)

    "return a never run outcome with the cancellation reason" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val reason = "bar"
      execution ! Cancel(reason)

      expectMsgType[NeverRun].reason should be (reason)
      expectTerminated(execution)
    }
  }

  "A waiting execution that is cancelled" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      self, "WaitingExecution"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "return a never run outcome with the cancellation reason" in {
      val reason = "bar1"
      execution ! Cancel(reason)

      expectMsgType[NeverRun].reason should be (reason)
      expectTerminated(execution)
    }
  }

  "An in progress execution that gets cancelled" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      self, "CancelledExecution"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      execution ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      within(2 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[Execution].stateName should be (Waiting)
        }
      }
    }

    "return an empty outcome again" in {
      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become in progress when notified to start" in {
      execution ! Start

      awaitAssert({
        execution.underlying.actor.asInstanceOf[Execution].stateName should be (InProgress)
      }, 2 seconds)
    }

    "return an interrupted outcome with the cancellation reason" in {
      val reason = "whatever"
      execution ! Cancel(reason)

      expectMsgType[Interrupted](5 seconds).reason should be (reason)
      expectTerminated(execution)
    }
  }

  "An in progress execution that times out by the queue" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      self, "ExecutionTimedOutByQueue"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      execution ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      within(2 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[Execution].stateName should be (Waiting)
        }
      }
    }

    "return an empty outcome again" in {
      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become in progress when notified to start" in {
      execution ! Start

      awaitAssert({
        execution.underlying.actor.asInstanceOf[Execution].stateName should be (InProgress)
      }, 1 second)
    }

    "return an never ending outcome when task queue notifies time out" in {
      execution.tell(TimeOut, taskQueue.ref)

      expectMsg(NeverEnding)
      expectTerminated(execution)
    }
  }

  "An execution that times out by itself" should {
    val expectedTimeout = 100 millis
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref, executionTimeout = Some(expectedTimeout)),
      self, "TimingOutExecution"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      execution ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      within(2 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[Execution].stateName should be (Waiting)
        }
      }
    }

    "return an empty outcome again" in {
      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "timeout right after notified to start" in {
      execution ! Start

      within(10 millis) {
        awaitAssert {
          execution.underlying.actor.asInstanceOf[Execution].stateName should be (InProgress)
        }
      }

      within(1 second) {
        taskQueue.expectMsgType[TaskQueue.TimeOut].taskId should be (task.id)
        taskQueue.reply(TimeOut)

        expectMsg(NeverEnding)
        expectTerminated(execution)
      }
    }
  }

  "An execution that is never acked by the queue" should {
    val expectedTimeout = 100 millis
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      Execution.props(planId, task, taskQueue.ref, expectedTimeout),
      self, "NeverAckedExecution"
    )
    watch(execution)

    "return an empty outcome" in {
      execution.underlying.actor.asInstanceOf[Execution].stateName should be (Sleeping)

      val outcome = (execution ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NotRunYet) }
    }

    "become cancelled after the ack timeout" in {
      execution ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)

      within(expectedTimeout, 500 millis) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      expectMsgType[NeverRun].reason should be ("Could not enqueue task!")
    }
  }

}
