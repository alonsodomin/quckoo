package io.chronos.scheduler.execution

import java.util.UUID

import akka.pattern._
import akka.testkit._
import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import io.chronos.scheduler.TestActorSystem
import io.chronos.scheduler.queue.TaskQueue
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

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

  val planId = UUID.randomUUID()
  val task = Task(id = UUID.randomUUID(), moduleId = TestModuleId, jobClass = TestJobClass)

  val parent = TestProbe()
  val taskQueue = TestProbe()

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A sleeping execution" should {
    val executionRef = TestActorRef(Execution.props(planId, task, taskQueue.ref), parent.ref, "SleepyExecution")

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Option[Outcome]]
      whenReady(outcome) { _ should be (None) }
    }

    "return None if it's cancelled" in {
      executionRef ! Cancel("foo")

      parent.expectMsg(None)
    }
  }

  "A new execution" should {
    val executionRef = TestActorRef(Execution.props(planId, task, taskQueue.ref), parent.ref, "WaitingExecution")

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return None if it's cancelled" in {
      executionRef ! Cancel("foo")

      parent.expectMsg(None)
    }
  }

}
