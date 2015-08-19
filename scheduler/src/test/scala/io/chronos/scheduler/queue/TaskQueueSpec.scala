package io.chronos.scheduler.queue

import java.util.UUID

import akka.actor.Address
import akka.pattern._
import akka.testkit._
import io.chronos.cluster.WorkerProtocol._
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import io.chronos.scheduler.execution.Execution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object TaskQueueSpec {

  final val TestMaxTaskTimeout = 5 minutes
  final val TestModuleId = ModuleId("com.example", "example", "test")
  final val TestJobClass = "com.example.Job"

}

class TaskQueueSpec extends TestKit(TestActorSystem("TaskQueueSpec")) with DefaultTimeout
  with WordSpecLike with BeforeAndAfterAll with ScalaFutures with Matchers {

  import TaskQueue._
  import TaskQueueSpec._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A TaskQueue" should {
    val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

    val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout))
    val workerId = UUID.randomUUID()
    val executionProbe = TestProbe()
    val workerProbe = TestProbe()

    "register workers and return their address" in {
      taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)

      whenReady((taskQueue ? GetWorkers).mapTo[Seq[Address]]) { addresses =>
        addresses should contain (workerProbe.ref.path.address)
      }
    }

    "notify the workers when a task is enqueued" in {
      taskQueue.tell(Enqueue(task), executionProbe.ref)

      workerProbe.expectMsg(TaskReady)
      executionProbe.expectMsgType[EnqueueAck].taskId should be (task.id)
    }

    "just return ack when the same task is enqueued" in {
      taskQueue.tell(Enqueue(task), executionProbe.ref)

      workerProbe.expectNoMsg()
      executionProbe.expectMsgType[EnqueueAck].taskId should be (task.id)
    }

    "dispatch task to worker on successful request and notify execution" in {
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      workerProbe.expectMsg(task)
      executionProbe.expectMsg(Execution.Start)
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
      executionProbe.expectMsgType[Execution.Finish].result should be (Right(taskResult))
    }
  }

  "A TaskQueue with an execution in progress" should {
    val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

    val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout))
    val workerId = UUID.randomUUID()
    val executionProbe = TestProbe()
    val workerProbe = TestProbe()

    /*workerProbe.ignoreMsg {
      case TaskReady | _: Task => true
      case _ => false
    }
    executionProbe.ignoreMsg {
      case _: EnqueueAck | Execution.Start => true
      case _ => false
    }*/

    "notify the execution when the worker fails" in {
      workerProbe.ignoreMsg {
        case TaskReady | _: Task => true
        case _ => false
      }
      executionProbe.ignoreMsg {
        case _: EnqueueAck | Execution.Start => true
        case _ => false
      }

      taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)
      taskQueue.tell(Enqueue(task), executionProbe.ref)
      taskQueue.tell(RequestTask(workerId), workerProbe.ref)

      val cause: TaskFailureCause = Right(new Exception("TEST EXCEPTION"))
      taskQueue.tell(TaskFailed(workerId, task.id, cause), workerProbe.ref)

      executionProbe.expectMsgType[Execution.Finish].result should be (Left(cause))
    }
  }

}
