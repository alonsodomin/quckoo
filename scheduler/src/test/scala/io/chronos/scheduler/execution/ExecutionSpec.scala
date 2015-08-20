package io.chronos.scheduler.execution

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.pattern._
import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import io.chronos.scheduler.queue.TaskQueue
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent._
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
  import system.dispatcher

  val planId = UUID.randomUUID()
  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A full running execution" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      parent.ref, "FullPathExecution"
    )

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return an empty outcome again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become in progress when notified to start" in {
      executionRef ! Start
    }

    "and send result to parent when is finished" in {
      val result: Int = 8392
      executionRef ! Finish(Right(result))

      parent.expectMsg(Success(result))
    }
  }

  "An execution cancelled while sleeping" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      parent.ref, "SleepingExecution"
    )

    "return a never run outcome with the cancellation reason" in {
      val reason = "bar"
      executionRef ! Cancel(reason)

      parent.expectMsgType[NeverRun].reason should be (reason)
    }
  }

  "A waiting execution that is cancelled" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      parent.ref, "WaitingExecution"
    )

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "and return a never run outcome with the cancellation reason" in {
      val reason = "bar"
      executionRef ! Cancel(reason)

      parent.expectMsgType[NeverRun].reason should be (reason)
    }
  }

  "An in progress execution that gets cancelled" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      parent.ref, "CancelledExecution"
    )

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return an empty outcome again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become in progress when notified to start" in {
      executionRef ! Start
    }

    "and return an interrupted outcome with the cancellation reason" in {
      val reason = "bar"
      executionRef ! Cancel(reason)

      parent.expectMsgType[Interrupted].reason should be (reason)
    }
  }

  "An in progress execution that times out" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref),
      parent.ref, "CancelledExecution"
    )

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return an empty outcome again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become in progress when notified to start" in {
      executionRef ! Start
    }

    "and return an never ending outcome when task notifies time out" in {
      executionRef.tell(TimeOut, taskQueue.ref)

      parent.expectMsg(NeverEnding)
    }
  }

  "An execution that times out by itself" should {
    val parent = TestProbe()
    val taskQueue = TestProbe()
    val executionRef = TestActorRef(
      Execution.props(planId, task, taskQueue.ref, Some(50 millis)),
      parent.ref, "TimingOutExecution"
    )

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return an empty outcome again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Outcome]
      whenReady(outcome) { _ should be (NoOutcomeYet) }
    }

    "timeout right after notified to start" in {
      executionRef ! Start

      val waitForTimeout = Future { blocking { TimeUnit.MILLISECONDS.sleep(100) } }
      whenReady(waitForTimeout) { _ =>
        //taskQueue.expectMsgType[TaskQueue.TimeOut](10 seconds).taskId should be (task.id)
        executionRef.tell(TimeOut, taskQueue.ref)

        parent.expectMsg(NeverEnding)
      }
    }
  }

}
