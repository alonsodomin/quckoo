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

  "A full running execution" should {
    val executionRef = TestActorRef(Execution.props(planId, task, taskQueue.ref), parent.ref, "FullPathExecution")

    "return an empty outcome" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Option[Outcome]]
      whenReady(outcome) { _ should be (None) }
    }

    "become Waiting and send enqueue to the task queue on a WakeUp event" in {
      executionRef ! WakeUp

      taskQueue.expectMsgType[TaskQueue.Enqueue].task should be (task)
    }

    "return an empty outcome again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Option[Outcome]]
      whenReady(outcome) { _ should be (None) }
    }

    "become in progress when notified to start" in {
      executionRef ! Start
    }

    "return an empty outcome once again" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Option[Outcome]]
      whenReady(outcome) { _ should be (None) }
    }

    val result: Int = 8392
    "send result to parent when is finished" in {
      executionRef ! Finish(Right(result))

      parent.expectMsg(Success(result))
    }

    "return last result when asked for" in {
      val outcome = (executionRef ? GetOutcome).mapTo[Option[Outcome]]
      whenReady(outcome) { _ should be (Some(Success(result))) }
    }
  }

}
