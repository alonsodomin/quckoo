/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor.Props
import akka.testkit._

import io.quckoo._
import io.quckoo.cluster.scheduler.TaskQueue.EnqueueAck
import io.quckoo.testkit.QuckooActorClusterSuite

import scala.concurrent.duration._

/**
  * Created by domingueza on 18/08/15.
  */
object ExecutionLifecycleSpec {

  final val TestArtifactId = ArtifactId("com.example", "example", "test")
  final val TestJobClass   = "com.example.Job"

}

class ExecutionLifecycleSpec
    extends QuckooActorClusterSuite("ExecutionLifecycleSpec") with ImplicitSender
    with DefaultTimeout {

  import ExecutionLifecycle._
  import ExecutionLifecycleSpec._
  import TaskExecution._

  // We need to use a multi-threaded dispatcher to be able to realiably test the FSM
  private def executionProps(planId: PlanId,
                             enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
                             maxEnqueueAttempts: Int = DefaultMaxEnqueueAttempts,
                             executionTimeout: Option[FiniteDuration] = None): Props =
    ExecutionLifecycle
      .props(planId, enqueueTimeout, maxEnqueueAttempts, executionTimeout)
      .withDispatcher("akka.actor.default-dispatcher")

  "An execution cancelled before enqueuing" should {
    val planId = PlanId(UUID.randomUUID())

    val enqueueTimeout = 2 seconds
    val lifecycle = TestActorRef[ExecutionLifecycle](
      executionProps(
        planId,
        enqueueTimeout,
        maxEnqueueAttempts = 2
      ),
      self,
      "non-enqueued-exec"
    )
    watch(lifecycle)

    "terminate when receiving the Cancel signal" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Cancel(Reason.UserRequest)

      val resultMsg = expectMsgType[Result]
      resultMsg.outcome shouldBe Outcome.NeverRun(Reason.UserRequest)

      expectTerminated(lifecycle)
    }
  }

  "An execution that fails to enqueue" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-1")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 1 second
    val lifecycle = TestActorRef[ExecutionLifecycle](
      executionProps(
        planId,
        enqueueTimeout,
        maxEnqueueAttempts = 2
      ),
      self,
      "enqueued-timedout-exec"
    )
    watch(lifecycle)

    "request to enqueue after receiving WakeUp signal" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)
      taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task

      lifecycle ! Get

      val execution = expectMsgType[TaskExecution]
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None

      lifecycle.underlyingActor.stateName shouldBe Enqueuing
    }

    "retry to enqueue after timeout and then give up" in {
      val maxToWait = enqueueTimeout * 2

      val enqueueMsg = within(enqueueTimeout, maxToWait) {
        taskQueue.expectMsgType[TaskQueue.Enqueue]
      }
      enqueueMsg.task shouldBe task

      val resultMsg = within(enqueueTimeout, maxToWait) {
        expectMsgType[Result]
      }
      resultMsg.outcome should matchPattern {
        case Outcome.NeverRun(_) =>
      }

      expectTerminated(lifecycle)
    }
  }

  "An execution that is cancelled before starting" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-2")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val lifecycle =
      TestActorRef[ExecutionLifecycle](
        executionProps(
          planId,
          enqueueTimeout,
          maxEnqueueAttempts = 2
        ),
        self,
        "cancelled-before-starting-exec"
      )
    watch(lifecycle)

    "move to Waiting state after receiving an enqueue ack" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)
      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Enqueuing)

        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Waiting)
      }

      val triggeredMsg = expectMsgType[Triggered]
      triggeredMsg.task shouldBe task

      val execution = within(100 millis) {
        lifecycle ! Get
        expectMsgType[TaskExecution]
      }
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None
    }

    "terminate when requested to be cancelled" in {
      lifecycle ! Cancel(Reason.UserRequest)

      within(2 seconds) {
        val resultMsg = expectMsgType[Result]
        resultMsg.outcome should be(Outcome.NeverRun(Reason.UserRequest))
      }
      expectTerminated(lifecycle)
    }
  }

  "An execution in progress" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-3")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val lifecycle =
      TestActorRef[ExecutionLifecycle](
        executionProps(
          planId,
          enqueueTimeout,
          maxEnqueueAttempts = 2
        ),
        self,
        "cancelled-while-in-progress-exec"
      )
    watch(lifecycle)

    "move to Waiting state after receiving an enqueue ack" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)

      val execution = within(100 millis) {
        lifecycle ! Get
        expectMsgType[TaskExecution]
      }
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None

      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Waiting)
      }

      val triggeredMsg = expectMsgType[Triggered]
      triggeredMsg.task shouldBe task
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(lifecycle, Start)
      awaitAssert(lifecycle.underlyingActor.stateName shouldBe Running)
    }

    "send Interrupted as result when requested to Cancel" in {
      lifecycle ! Cancel(Reason.UserRequest)

      within(2 seconds) {
        val resultMsg = expectMsgType[Result]
        resultMsg.outcome should be(Outcome.Interrupted(Reason.UserRequest))
      }
      expectTerminated(lifecycle)
    }
  }

  "An execution that times out while running" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-4")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout   = 5 seconds
    val executionTimeout = 1 second
    val lifecycle =
      TestActorRef[ExecutionLifecycle](
        executionProps(
          planId,
          enqueueTimeout,
          maxEnqueueAttempts = 2,
          Some(executionTimeout)
        ),
        self,
        "timed-out-while-in-progress-exec"
      )
    watch(lifecycle)

    "move to Waiting state after receiving an enqueue ack" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)

      val execution = within(100 millis) {
        lifecycle ! Get
        expectMsgType[TaskExecution]
      }
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None

      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Waiting)
      }

      val triggeredMsg = expectMsgType[Triggered]
      triggeredMsg.task shouldBe task
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(lifecycle, Start)
      awaitAssert(lifecycle.underlyingActor.stateName shouldBe Running)
    }

    "request to the queue to time out the task" in {
      val timeoutMsg = within(executionTimeout + (1 second)) {
        taskQueue.expectMsgType[TaskQueue.TimeOut]
      }
      timeoutMsg.taskId should be(task.id)
    }

    "send NeverEnding as result when the queue replies" in {
      taskQueue.reply(TimeOut)

      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }
      resultMsg.outcome shouldBe Outcome.NeverEnding
      expectTerminated(lifecycle)
    }
  }

  "An execution that fails while running" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-5")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val lifecycle =
      TestActorRef[ExecutionLifecycle](
        executionProps(
          planId,
          enqueueTimeout,
          maxEnqueueAttempts = 2
        ),
        self,
        "fails-while-in-progress-exec"
      )
    watch(lifecycle)

    "move to Waiting state after receiving an enqueue ack" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)

      val execution = within(100 millis) {
        lifecycle ! Get
        expectMsgType[TaskExecution]
      }
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None

      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Waiting)
      }

      val triggeredMsg = expectMsgType[Triggered]
      triggeredMsg.task shouldBe task
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(lifecycle, Start)
      awaitAssert(lifecycle.underlyingActor.stateName shouldBe Running)
    }

    "reply with the fault that caused the failure" in {
      val fault = ExceptionThrown.from(new RuntimeException("TEST EXCEPTION"))

      taskQueue.send(lifecycle, Finish(Some(fault)))
      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }

      resultMsg.outcome shouldBe TaskExecution.Outcome.Failure(fault)
      expectTerminated(lifecycle)
    }
  }

  "An execution that completes successfully" should {
    val planId = PlanId(UUID.randomUUID())
    val task   = Task(TaskId(UUID.randomUUID()), JobPackage.jar(TestArtifactId, TestJobClass))

    val taskQueue          = TestProbe("queue-6")
    val taskQueueSelection = system.actorSelection(taskQueue.ref.path)

    val enqueueTimeout = 5 seconds
    val lifecycle = TestActorRef[ExecutionLifecycle](
      executionProps(
        planId,
        enqueueTimeout,
        maxEnqueueAttempts = 2
      ),
      self,
      "successful-exec"
    )
    watch(lifecycle)

    "move to Waiting state after receiving an enqueue ack" in {
      lifecycle.underlyingActor.stateName shouldBe Sleeping

      lifecycle ! Awake(task, taskQueueSelection)

      val execution = within(100 millis) {
        lifecycle ! Get
        expectMsgType[TaskExecution]
      }
      execution.planId shouldBe planId
      execution.task shouldBe task
      execution.outcome shouldBe None

      within(enqueueTimeout) {
        taskQueue.expectMsgType[TaskQueue.Enqueue].task shouldBe task
        taskQueue.reply(EnqueueAck(task.id))

        awaitAssert(lifecycle.underlyingActor.stateName shouldBe Waiting)
      }

      val triggeredMsg = expectMsgType[Triggered]
      triggeredMsg.task shouldBe task
    }

    "move to InProgress after receiving the Start signal" in {
      taskQueue.send(lifecycle, Start)
      awaitAssert(lifecycle.underlyingActor.stateName shouldBe Running)
    }

    "reply Success when it completes" in {
      taskQueue.send(lifecycle, Finish(None))
      val resultMsg = within(1 second) {
        expectMsgType[Result]
      }

      resultMsg.outcome shouldBe TaskExecution.Outcome.Success
      expectTerminated(lifecycle)
    }
  }

}
