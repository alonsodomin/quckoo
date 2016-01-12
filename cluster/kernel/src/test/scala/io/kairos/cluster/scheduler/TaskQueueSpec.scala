package io.kairos.cluster.scheduler

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.testkit._
import io.kairos.cluster.protocol.WorkerProtocol._
import io.kairos.cluster.scheduler.execution.ExecutionFSM
import io.kairos.cluster.{Task, TaskFailureCause}
import io.kairos.id.ArtifactId
import io.kairos.protocol.ExceptionThrown
import io.kairos.test.TestActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent._
import scala.concurrent.duration._
import scalaz.NonEmptyList

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object TaskQueueSpec {

  final val TestMaxTaskTimeout = 5 minutes
  final val TestArtifactId = ArtifactId("com.example", "example", "test")
  final val TestJobClass = "com.example.Job"

}

class TaskQueueSpec extends TestKit(TestActorSystem("TaskQueueSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {

  import TaskQueue._
  import TaskQueueSpec._
  import system.dispatcher

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A TaskQueue" should {
    val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

    val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout), "happyQueue")
    val workerId = UUID.randomUUID()
    val executionProbe = TestProbe("happyExec")
    val workerProbe = TestProbe("happyWorker")

    "register workers and return their address" in {
      taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)

      within(5 seconds) {
        taskQueue ! GetWorkers

        awaitAssert {
          val msg = expectMsgType[Workers]
          msg.locations should contain (workerProbe.ref.path.address)
        }
      }
    }

    "notify the workers when a task is enqueued" in {
      taskQueue.tell(Enqueue(task), executionProbe.ref)

      workerProbe.expectMsg(TaskReady)
      executionProbe.expectMsgType[EnqueueAck].taskId should be (task.id)
    }

    "dispatch task to worker on successful request and notify execution" in {
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      val returnedTask = workerProbe.expectMsgType[Task]
      returnedTask should be(task)

      executionProbe.expectMsg[ExecutionFSM.Command](ExecutionFSM.Start)
    }

    "ignore a task request from a busy worker" in {
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      workerProbe.expectNoMsg()
      executionProbe.expectNoMsg()
    }

    "not dispatch work to another worker when there is no more pending" in {
      val otherWorkerId = UUID.randomUUID()
      val otherWorkerProbe = TestProbe("otherWorker")
      taskQueue.tell(RegisterWorker(otherWorkerId), otherWorkerProbe.ref)

      taskQueue.tell(RequestTask(otherWorkerId), otherWorkerProbe.ref)

      otherWorkerProbe.expectNoMsg()
      executionProbe.expectNoMsg()
    }

    "resend task result to execution when worker finishes" in {
      val taskResult: Int = 26
      taskQueue.tell(TaskDone(workerId, task.id, taskResult), workerProbe.ref)

      workerProbe.expectMsgType[TaskDoneAck].taskId should be (task.id)
      executionProbe.expectMsgType[ExecutionFSM.Finish].result should be (Right(taskResult))
    }
  }

  "A TaskQueue with an execution in progress" should {
    val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout), "willFailQueue")

    "notify an error in the execution when the worker fails" in {
      val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

      val failingWorkerId = UUID.randomUUID()
      val failingExec = TestProbe("failingExec")
      val failingWorker = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(failingWorkerId), failingWorker.ref)
      taskQueue.tell(Enqueue(task), failingExec.ref)

      failingExec.expectMsgType[EnqueueAck]
      failingWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(failingWorkerId), failingWorker.ref)
      failingWorker.expectMsg(task)
      failingExec.expectMsg(ExecutionFSM.Start)

      val cause: TaskFailureCause = NonEmptyList(ExceptionThrown(new Exception("TEST EXCEPTION")))
      taskQueue.tell(TaskFailed(failingWorkerId, task.id, cause), failingWorker.ref)

      failingExec.expectMsgType[ExecutionFSM.Finish].result should be(Left(cause))
    }

    "perform a timeout if the execution does notify it" in {
      val taskTimeout = 1 seconds
      val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

      val timingOutWorkerId = UUID.randomUUID()
      val timingOutExec = TestProbe("failingExec")
      val timingOutWorker = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(timingOutWorkerId), timingOutWorker.ref)
      taskQueue.tell(Enqueue(task), timingOutExec.ref)

      timingOutExec.expectMsgType[EnqueueAck]
      timingOutWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(timingOutWorkerId), timingOutWorker.ref)
      timingOutWorker.expectMsg(task)
      timingOutExec.expectMsg[ExecutionFSM.Command](ExecutionFSM.Start)

      taskQueue.tell(TimeOut(task.id), timingOutExec.ref)
      timingOutExec.expectMsg[ExecutionFSM.Command](ExecutionFSM.TimeOut)
    }

  }

  "A task queue with a short timeout" should {
    val taskQueue = TestActorRef(TaskQueue.props(100 millis), "willTimeoutQueue")

    "notify a timeout if the worker doesn't reply in between the task timeout" in {
      val task = Task(id = UUID.randomUUID(), artifactId = TestArtifactId, jobClass = TestJobClass)

      val timingOutWorkerId = UUID.randomUUID()
      val timingOutExec = TestProbe("failingExec")
      val timingOutWorker = TestProbe("failingWorker")

      taskQueue.tell(RegisterWorker(timingOutWorkerId), timingOutWorker.ref)
      taskQueue.tell(Enqueue(task), timingOutExec.ref)

      timingOutExec.expectMsgType[EnqueueAck]
      timingOutWorker.expectMsg(TaskReady)

      taskQueue.tell(RequestTask(timingOutWorkerId), timingOutWorker.ref)
      timingOutWorker.expectMsg(task)
      timingOutExec.expectMsg(ExecutionFSM.Start)

      val waitForTimeout = Future { blocking { TimeUnit.MILLISECONDS.sleep(100) } }
      whenReady(waitForTimeout) { _ =>
        timingOutExec.expectMsg[ExecutionFSM.Command](ExecutionFSM.TimeOut)
      }
    }
  }

}
