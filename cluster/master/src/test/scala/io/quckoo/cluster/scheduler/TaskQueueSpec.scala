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
import java.util.concurrent.TimeUnit

import akka.testkit._

import io.quckoo._
import io.quckoo.cluster.protocol._
import io.quckoo.testkit.QuckooActorClusterSuite

import org.scalatest.concurrent.ScalaFutures

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 18/08/15.
  */
object TaskQueueSpec {

  final val TestMaxTaskTimeout = 5 minutes
  final val TestArtifactId     = ArtifactId("com.example", "example", "test")
  final val TestJobClass       = "com.example.Job"

}

class TaskQueueSpec
    extends QuckooActorClusterSuite("TaskQueueSpec") with ImplicitSender with ScalaFutures {

  import TaskQueue._
  import TaskQueueSpec._
  import system.dispatcher

  "A TaskQueue" should {
    val task =
      Task(
        TaskId(UUID.randomUUID()),
        JobPackage.jar(artifactId = TestArtifactId, jobClass = TestJobClass)
      )

    val taskQueue =
      TestActorRef(TaskQueue.props(TestMaxTaskTimeout), "happyQueue")
    val workerId       = NodeId(UUID.randomUUID())
    val executionProbe = TestProbe("happyExec")
    val workerProbe    = TestProbe("happyWorker")

    "register workers and return their address" in {
      taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)

      within(5 seconds) {
        taskQueue ! GetWorkers

        awaitAssert {
          val msg = expectMsgType[Workers]
          msg.locations should contain(workerProbe.ref.path.address)
        }
      }
    }

    "notify the workers when a task is enqueued" in {
      taskQueue.tell(Enqueue(task), executionProbe.ref)

      workerProbe.expectMsg(TaskReady)
      executionProbe.expectMsgType[EnqueueAck].taskId should be(task.id)
    }

    "dispatch task to worker on successful request and notify execution" in {
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      val returnedTask = workerProbe.expectMsgType[Task]
      returnedTask should be(task)

      executionProbe.expectMsg[ExecutionLifecycle.Command](ExecutionLifecycle.Start)
    }

    "ignore a task request from a busy worker" in {
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      workerProbe.expectNoMessage(500 millis)
      executionProbe.expectNoMessage(500 millis)
    }

    "not dispatch work to another worker when there is no more pending" in {
      val otherWorkerId    = NodeId(UUID.randomUUID())
      val otherWorkerProbe = TestProbe("otherWorker")

      taskQueue.tell(RegisterWorker(otherWorkerId), otherWorkerProbe.ref)
      taskQueue.tell(RequestTask(otherWorkerId), otherWorkerProbe.ref)

      otherWorkerProbe.expectNoMessage(500 millis)
      executionProbe.expectNoMessage(500 millis)
    }

    "resend task result to execution when worker finishes" in {
      val taskResult: Int = 26
      taskQueue.tell(TaskDone(workerId, task.id, taskResult), workerProbe.ref)

      workerProbe.expectMsgType[TaskDoneAck].taskId should be(task.id)
      executionProbe.expectMsgType[ExecutionLifecycle.Finish].fault should be(None)
    }
  }

  "A TaskQueue with an execution in progress" should {
    val taskQueue =
      TestActorRef(TaskQueue.props(TestMaxTaskTimeout), "willFailQueue")

    "notify an error in the execution when the worker fails" in {
      val task = Task(
        TaskId(UUID.randomUUID()),
        JobPackage.jar(artifactId = TestArtifactId, jobClass = TestJobClass)
      )

      val failingWorkerId = NodeId(UUID.randomUUID())
      val failingExec     = TestProbe("failingExec")
      val failingWorker   = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(failingWorkerId), failingWorker.ref)
      taskQueue.tell(Enqueue(task), failingExec.ref)

      failingExec.expectMsgType[EnqueueAck]
      failingWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(failingWorkerId), failingWorker.ref)
      failingWorker.expectMsg(task)
      failingExec.expectMsg(ExecutionLifecycle.Start)

      val cause: QuckooError =
        ExceptionThrown.from(new Exception("TEST EXCEPTION"))
      taskQueue.tell(TaskFailed(failingWorkerId, task.id, cause), failingWorker.ref)

      failingExec.expectMsgType[ExecutionLifecycle.Finish].fault should be(Some(cause))
    }

    "perform a timeout if the execution does notify it" in {
      val taskTimeout = 1 seconds
      val task = Task(
        TaskId(UUID.randomUUID()),
        JobPackage.jar(artifactId = TestArtifactId, jobClass = TestJobClass)
      )

      val timingOutWorkerId = NodeId(UUID.randomUUID())
      val timingOutExec     = TestProbe("failingExec")
      val timingOutWorker   = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(timingOutWorkerId), timingOutWorker.ref)
      taskQueue.tell(Enqueue(task), timingOutExec.ref)

      timingOutExec.expectMsgType[EnqueueAck]
      timingOutWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(timingOutWorkerId), timingOutWorker.ref)
      timingOutWorker.expectMsg(task)
      timingOutExec.expectMsg[ExecutionLifecycle.Command](ExecutionLifecycle.Start)

      taskQueue.tell(TimeOut(task.id), timingOutExec.ref)
      timingOutExec.expectMsg[ExecutionLifecycle.Command](ExecutionLifecycle.TimeOut)
    }

  }

  "A task queue with a short timeout" should {
    val taskQueue =
      TestActorRef(TaskQueue.props(100 millis), "willTimeoutQueue")

    "notify a timeout if the worker doesn't reply in between the task timeout" in {
      val task = Task(
        TaskId(UUID.randomUUID()),
        JobPackage.jar(artifactId = TestArtifactId, jobClass = TestJobClass)
      )

      val timingOutWorkerId = NodeId(UUID.randomUUID())
      val timingOutExec     = TestProbe("failingExec")
      val timingOutWorker   = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(timingOutWorkerId), timingOutWorker.ref)
      taskQueue.tell(Enqueue(task), timingOutExec.ref)

      timingOutExec.expectMsgType[EnqueueAck]
      timingOutWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(timingOutWorkerId), timingOutWorker.ref)
      timingOutWorker.expectMsg(task)
      timingOutExec.expectMsg(ExecutionLifecycle.Start)

      val waitForTimeout = Future {
        blocking { TimeUnit.MILLISECONDS.sleep(100) }
      }
      whenReady(waitForTimeout) { _ =>
        timingOutExec.expectMsg[ExecutionLifecycle.Command](ExecutionLifecycle.TimeOut)
      }
    }
  }

}
