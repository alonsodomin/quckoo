package io.chronos.scheduler.execution

import java.util.UUID

import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import io.chronos.scheduler.queue.TaskQueue
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by domingueza on 18/08/15.
 */
object ExecutionSpec {

  val TestModuleId = ModuleId("com.example", "example", "test")
  val TestJobClass = "com.example.Job"

}

class ExecutionSpec extends TestKit(TestActorSystem("ExecutionSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers {

  import Execution._
  import ExecutionSpec._

  val planId = UUID.randomUUID()
  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)
  val taskQueue = TestProbe()

  val executionRef = TestActorRef(Execution.props(planId, task, taskQueue.ref))

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution" should "become Waiting and send enqueue to the task queue on a WakeUp event" in {
    executionRef ! WakeUp

    taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
  }

}
