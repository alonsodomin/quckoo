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

package io.quckoo.worker

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.cluster.client.ClusterClient.SendToAll
import akka.testkit._

import io.quckoo.Task
import io.quckoo.cluster.protocol._
import io.quckoo.fault.{ExceptionThrown, MissingDependencies, UnresolvedDependency}
import io.quckoo.id.ArtifactId
import io.quckoo.resolver.{Artifact, Resolver}

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent._
import scala.concurrent.duration._
import scalaz.NonEmptyList

/**
 * Created by domingueza on 21/08/15.
 */
object WorkerSpec {

  final val TestSchedulerPath = "/user/quckoo/scheduler"

  final val FooJobClass = "com.example.FooClass"
  final val FooArtifactId = ArtifactId("com.example", "foo", "latest")
  final val FooArtifact = Artifact(FooArtifactId, Seq.empty)

}

class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {

  import WorkerSpec._
  import system.dispatcher

  val clusterClientProbe = TestProbe()

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A worker" should {
    val resolverProbe = TestProbe()
    val resolverProps = TestActors.forwardActorProps(resolverProbe.ref)

    val executorProbe = TestProbe()
    val executorProps = TestActors.forwardActorProps(executorProbe.ref)

    val task = Task(UUID.randomUUID(), FooArtifactId, FooJobClass)

    val ackTimeout = 1 second
    val worker = TestActorRef(Worker.props(clusterClientProbe.ref, resolverProps, executorProps, 1 day, ackTimeout))

    "auto-register itself with the task queue" in {
      val registration = clusterClientProbe.expectMsgType[SendToAll]
      registration.path should be (TestSchedulerPath)
      registration.msg should matchPattern { case RegisterWorker(_) => }
    }

    "request work to the task queue when the queue says that there are pending tasks" in {
      worker ! TaskReady

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case RequestTask(_) => }
    }

    "download the artifact for a task that arrives to it when it's idle" in {
      worker ! task

      resolverProbe.expectMsgType[Resolver.Download].artifactId should be (task.artifactId)
    }

    "reject subsequent tasks if we are busy" in {
      val anotherTask = Task(UUID.randomUUID(), FooArtifactId, FooJobClass)
      worker ! anotherTask

      executorProbe.expectNoMsg(500 millis)
    }

    "send the task to the execution when it has been successfully resolved" in {
      resolverProbe.reply(Resolver.ArtifactResolved(FooArtifact))

      val executeMsg = executorProbe.expectMsgType[JobExecutor.Execute]
      executeMsg.task should be (task)
      executeMsg.artifact should be (FooArtifact)
    }

    "notify the task queue when the worker completes with a result" in {
      val taskId = task.id
      val result = 38283
      executorProbe.send(worker, JobExecutor.Completed(result))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, `result`) => }
    }

    "resend task done notification if queue doesn't reply whiting the ack timeout" in {
      val taskId = task.id

      val waitingForTimeout = Future { blocking { TimeUnit.SECONDS.sleep(ackTimeout.toSeconds) } }
      whenReady(waitingForTimeout, Timeout(Span(2, Seconds))) { _ =>
        val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
        queueMsg.path should be (TestSchedulerPath)
        queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, _) => }
      }
    }

    "ignore an ack from the queue for a different task id" in {
      val taskId = task.id
      val anotherTaskId = UUID.randomUUID()

      worker ! TaskDoneAck(anotherTaskId)

      val waitingForTimeout = Future { blocking { TimeUnit.SECONDS.sleep(ackTimeout.toSeconds) } }
      whenReady(waitingForTimeout, Timeout(Span(2, Seconds))) { _ =>
        val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
        queueMsg.path should be (TestSchedulerPath)
        queueMsg.msg should matchPattern { case TaskDone(_, `taskId`, _) => }
      }
    }

    "ask for another task and become idle when task queue acks the done notification" in {
      val taskId = task.id

      worker ! TaskDoneAck(taskId)

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case RequestTask(_) => }
    }

    "reply with a failure message when can not resolve the artifact of a task" in {
      val taskId = task.id
      val dependencyError = UnresolvedDependency(ArtifactId("com.example", "bar", "latest"))
      val expectedFault = MissingDependencies(NonEmptyList(dependencyError))

      worker ! task

      resolverProbe.expectMsgType[Resolver.Download].artifactId should be (task.artifactId)
      resolverProbe.reply(Resolver.ResolutionFailed(task.artifactId, expectedFault))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskFailed(_, `taskId`, `expectedFault`) => }
    }

    "send another task to the executor after downloading the artifact" in {
      worker ! task

      resolverProbe.expectMsgType[Resolver.Download].artifactId should be (task.artifactId)
      resolverProbe.reply(Resolver.ArtifactResolved(FooArtifact))

      val executeMsg = executorProbe.expectMsgType[JobExecutor.Execute]
      executeMsg.task should be (task)
      executeMsg.artifact should be (FooArtifact)
    }

    "become idle again if the executor notifies failure when running the task" in {
      val taskId = task.id
      val cause = new Exception("TEST EXCEPTION")

      val expectedError = ExceptionThrown.from(cause)
      executorProbe.send(worker, JobExecutor.Failed(expectedError))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskFailed(_, `taskId`, `expectedError`) => }
    }

  }

}
