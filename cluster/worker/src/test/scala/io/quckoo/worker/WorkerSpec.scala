package io.quckoo.worker

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.cluster.client.ClusterClient.SendToAll
import akka.testkit._
import io.quckoo.Task
import io.quckoo.cluster.protocol._
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.ArtifactId
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

  final val TestJobClass = "com.example.FooClass"
  final val TestArtifactId = ArtifactId("com.example", "foo", "latest")

}

class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {

  import WorkerSpec._
  import system.dispatcher

  val clusterClientProbe = TestProbe()

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A worker" should {
    val executorProbe = TestProbe()
    val executorProps = TestActors.forwardActorProps(executorProbe.ref)

    val task = Task(UUID.randomUUID(), TestArtifactId, Map.empty, TestJobClass)

    val ackTimeout = 1 second
    val worker = TestActorRef(Worker.props(clusterClientProbe.ref, executorProps, 1 day, ackTimeout))

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

    "instruct the executor to execute a task that arrives to it when it's idle" in {
      worker ! task

      executorProbe.expectMsgType[JobExecutor.Execute].task should be (task)
    }

    "not overwhelm the executor with more tasks if we are busy" in {
      val anotherTask = Task(UUID.randomUUID(), TestArtifactId, Map.empty, TestJobClass)
      worker ! anotherTask

      executorProbe.expectNoMsg(500 millis)
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
      whenReady(waitingForTimeout, Timeout(Span(1, Seconds))) { _ =>
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
      whenReady(waitingForTimeout, Timeout(Span(1, Seconds))) { _ =>
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

    "send another task to the executor" in {
      worker ! task

      executorProbe.expectMsgType[JobExecutor.Execute].task should be (task)
    }

    "become idle again if the executor notifies failure when running the task" in {
      val taskId = task.id
      val cause = new Exception("TEST EXCEPTION")

      val expectedError = ExceptionThrown(cause)
      executorProbe.send(worker, JobExecutor.Failed(NonEmptyList(expectedError)))

      val queueMsg = clusterClientProbe.expectMsgType[SendToAll]
      queueMsg.path should be (TestSchedulerPath)
      queueMsg.msg should matchPattern { case TaskFailed(_, `taskId`, NonEmptyList(`expectedError`)) => }
    }

    "send one more task to the executor" in {
      worker ! task

      executorProbe.expectMsgType[JobExecutor.Execute].task should be (task)
    }
  }

}