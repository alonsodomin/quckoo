/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.worker.core

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorRefFactory
import akka.cluster.client.ClusterClient.SendToAll
import akka.testkit._

import io.quckoo._
import io.quckoo.reflect.Artifact
import io.quckoo.cluster.protocol._
import io.quckoo.testkit.QuckooActorSuite
import io.quckoo.worker.executor.PureResolver

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by domingueza on 21/08/15.
 */
object WorkerSpec {

  final val TestSchedulerPath = "/user/quckoo/scheduler"

  final val FooJobClass = "com.example.FooClass"
  final val FooArtifactId = ArtifactId("com.example", "foo", "latest")
  final val FooArtifact = Artifact(FooArtifactId, List.empty)

  final val TestTaskId = TaskId(UUID.randomUUID())
  final val TestTask = Task(TestTaskId, JobPackage.jar(FooArtifactId, FooJobClass))

}

class WorkerSpec extends QuckooActorSuite("WorkerSpec")
    with ImplicitSender
    with ScalaFutures {

  import WorkerSpec._
  import system.dispatcher

  val clusterClientProbe = TestProbe()

  "A worker" should {
    val resolver = new PureResolver(FooArtifact)

    val executorProbe = TestProbe()
    val executorProps = TestActors.forwardActorProps(executorProbe.ref)

    val executorProvider = new TaskExecutorProvider {
      override def executorFor(context: WorkerContext, task: Task)
                              (implicit actorRefFactory: ActorRefFactory) = {
        actorRefFactory.actorOf(executorProps)
      }
    }

    val ackTimeout = 1 second
    val worker = TestActorRef(
      Worker.props(clusterClientProbe.ref, resolver, executorProvider, 1 day, ackTimeout),
      "sucessful-worker"
    )

    "auto-register itself with the master" in {
      val registration = clusterClientProbe.expectMsgType[SendToAll]
      registration.path should be (TestSchedulerPath)
      registration.msg should matchPattern { case RegisterWorker(_) => }
    }

    "request work to the master when the queue says that there are pending tasks" in {
      worker ! TaskReady

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case RequestTask(_) => }
    }

    "create an executor for a task that arrives to it when it's idle" in {
      worker ! TestTask

      executorProbe.expectMsg(TaskExecutor.Run)
    }

    "reject subsequent tasks if it's busy" in {
      val anotherTask = Task(TaskId(UUID.randomUUID()), JobPackage.jar(FooArtifactId, FooJobClass))
      worker ! anotherTask

      executorProbe.expectNoMsg(500 millis)
    }

    "notify the master when the worker completes with a result" in {
      val taskId = TestTask.id
      val result = 38283
      executorProbe.send(worker, TaskExecutor.Completed(result))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, `result`) => }
    }

    "resend task done notification if queue doesn't reply whiting the ack timeout" in {
      val taskId = TestTask.id

      val waitingForTimeout = Future { blocking { TimeUnit.SECONDS.sleep(ackTimeout.toSeconds) } }
      whenReady(waitingForTimeout, Timeout(Span(2, Seconds))) { _ =>
        val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
        queueMsg.path should be (TestSchedulerPath)
        queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, _) => }
      }
    }

    "ignore an ack from the master for a different task id" in {
      val taskId = TestTask.id
      val anotherTaskId = TaskId(UUID.randomUUID())

      worker ! TaskDoneAck(anotherTaskId)

      val waitingForTimeout = Future { blocking { TimeUnit.SECONDS.sleep(ackTimeout.toSeconds) } }
      whenReady(waitingForTimeout, Timeout(Span(2, Seconds))) { _ =>
        val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
        queueMsg.path should be (TestSchedulerPath)
        queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, _) => }
      }
    }

    "ask for another task and become idle when the master acks the done notification" in {
      val taskId = TestTask.id

      worker ! TaskDoneAck(taskId)

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case RequestTask(_) => }
    }

    "become idle again if the executor notifies failure when running the task" in {
      worker ! TestTask

      val taskId = TestTask.id
      val cause = new Exception("TEST EXCEPTION")

      val expectedError = ExceptionThrown.from(cause)
      executorProbe.expectMsg(TaskExecutor.Run)
      executorProbe.send(worker, TaskExecutor.Failed(expectedError))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskFailed(_, `taskId`, `expectedError`) => }
    }

  }

}
