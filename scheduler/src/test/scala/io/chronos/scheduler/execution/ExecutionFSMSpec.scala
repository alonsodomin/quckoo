package io.chronos.scheduler.execution

import java.util.UUID

import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import io.chronos.scheduler.TaskQueue
import io.chronos.scheduler.TaskQueue.EnqueueAck
import io.chronos.test.{ImplicitClock, TestActorSystem}
import org.scalatest.{BeforeAndAfterAll, Ignore, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by domingueza on 18/08/15.
 */
object ExecutionFSMSpec {

  val TestModuleId = ModuleId("com.example", "example", "test")
  val TestJobClass = "com.example.Job"

}

@Ignore
class ExecutionFSMSpec extends TestKit(TestActorSystem("ExecutionFSMSpec")) with ImplicitSender with DefaultTimeout
  with WordSpecLike with BeforeAndAfterAll with Matchers with ImplicitClock {

  import Execution._
  import ExecutionFSM._
  import ExecutionFSMSpec._

  val planId = UUID.randomUUID()
  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A full running execution" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref),
      self, "FullPathExecution"
    )
    watch(execution)

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        execution ! WakeUp
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      taskQueue.reply(EnqueueAck(task.id))
    }

    "become in progress when notified to start" in {
      execution ! Start
    }

    "send result to parent when is finished" in {
      val result: Int = 8392
      execution ! Finish(Right(result))

      expectMsg(Result(Success(result)))
      expectTerminated(execution)
    }
  }

  "An execution cancelled while sleeping" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref),
      self, "SleepingExecution"
    )
    watch(execution)

    "return a never run outcome with the cancellation reason" in {
      execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Scheduled)

      val reason = "bar"
      execution ! Cancel(reason)

      expectMsg(Result(NeverRun(reason)))
      expectTerminated(execution)
    }
  }

  "A waiting execution that is cancelled" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref),
      self, "WaitingExecution"
    )
    watch(execution)

    "return a never run outcome with the cancellation reason" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        val reason = "bar1"
        execution ! Cancel(reason)

        expectMsg(Result(NeverRun(reason)))
      }
      expectTerminated(execution)
    }
  }

  "An in progress execution that gets cancelled" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref),
      self, "CancelledExecution"
    )
    watch(execution)

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        execution ! WakeUp
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Scheduled)

      within(5 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Waiting)
        }
      }

      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }
    }

    "become in progress when notified to start" in {
      within(5 seconds) {
        execution ! Start

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (InProgress)
        }
      }
    }

    "return an interrupted outcome with the cancellation reason" in {
      val reason = "whatever"
      execution ! Cancel(reason)

      expectMsg(Result(Interrupted(reason)))
      expectTerminated(execution)
    }
  }

  "An in progress execution that times out by the queue" should {
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref),
      self, "ExecutionTimedOutByQueue"
    )
    watch(execution)

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        execution ! WakeUp
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Scheduled)

      within(5 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Waiting)
        }
      }

      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }
    }

    "become in progress when notified to start" in {
      within(5 seconds) {
        execution ! Start

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (InProgress)
        }
      }
    }

    "return an never ending outcome when task queue notifies time out" in {
      execution.tell(TimeOut, taskQueue.ref)

      expectMsg(Result(NeverEnding))
      expectTerminated(execution)
    }
  }

  "An execution that times out by itself" should {
    val expectedTimeout = 100 millis
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref, executionTimeout = Some(expectedTimeout)),
      self, "TimingOutExecution"
    )
    watch(execution)

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        execution ! WakeUp
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Scheduled)

      within(5 seconds) {
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (Waiting)
        }
      }

      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }
    }

    "timeout right after notified to start" in {
      within(5 seconds) {
        execution ! Start

        awaitAssert {
          execution.underlying.actor.asInstanceOf[ExecutionFSM].stateName should be (InProgress)
        }
      }

      within(1 second) {
        taskQueue.expectMsgType[TaskQueue.TimeOut].taskId should be (task.id)
        taskQueue.reply(TimeOut)

        expectMsg(Result(NeverEnding))
        expectTerminated(execution)
      }
    }
  }

  "An execution that is never acked by the queue" should {
    val expectedTimeout = 100 millis
    val taskQueue = TestProbe()
    val execution = TestActorRef(
      ExecutionFSM.props(planId, task, taskQueue.ref, expectedTimeout),
      self, "NeverAckedExecution"
    )
    watch(execution)

    "become cancelled after the ack timeout" in {
      within(1 second) {
        execution ! GetExecution
        expectMsg(NotRunYet)
      }

      within(1 second) {
        execution ! WakeUp
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      within(expectedTimeout, 500 millis) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
      }

      expectMsg(Result(NeverRun(s"Could not enqueue task! taskId=${task.id}")))
    }
  }

}