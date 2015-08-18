package io.chronos.scheduler.queue

import java.util.UUID

import akka.actor.Address
import akka.pattern._
import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.cluster.WorkerProtocol._
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object TaskQueueSpec {

  final val TestMaxTaskTimeout = 5 seconds
  final val TestModuleId = ModuleId("com.example", "example", "test")
  final val TestJobClass = "com.example.Job"

}

class TaskQueueSpec extends TestKit(TestActorSystem("TaskQueueSpec")) with ImplicitSender with DefaultTimeout
  with WordSpecLike with BeforeAndAfterAll with ScalaFutures with Matchers {

  import TaskQueue._
  import TaskQueueSpec._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)
  val taskQueue = TestActorRef(TaskQueue.props(TestMaxTaskTimeout))

  "A TaskQueue" should {
    val workerId = UUID.randomUUID()
    val workerProbe = TestProbe("workerProbe")
    taskQueue.tell(RegisterWorker(workerId), workerProbe.ref)

    "register workers and return their address" in {
      whenReady((taskQueue ? GetWorkers).mapTo[Seq[Address]]) { addresses =>
        addresses should contain (workerProbe.ref.path.address)
      }
    }

    "notify the workers when a task is enqueued" in {
      taskQueue ! Enqueue(task)

      workerProbe.expectMsg(TaskReady)
      expectMsgType[EnqueueAck].taskId should be (task.id)
    }
  }

}
