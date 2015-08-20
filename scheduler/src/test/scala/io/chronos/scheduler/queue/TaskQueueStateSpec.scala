package io.chronos.scheduler.queue

import java.util.UUID

import io.chronos.cluster.Task
import io.chronos.id.ModuleId
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by aalonsodominguez on 21/08/15.
 */
class TaskQueueStateSpec extends FlatSpec with Matchers {
  import TaskQueueState._

  final val TestModuleId = ModuleId("com.example", "example", "test")
  final val TestJobClass = "com.example.Job"
  final val TaskId = UUID.randomUUID()

  var queueState = TaskQueueState.empty

  "A TaskQueueState" should "enqueue accepted tasks" in {
    val task = Task(id = TaskId, moduleId = TestModuleId, jobClass = TestJobClass)

    queueState = queueState.updated(TaskAccepted(task, null))

    queueState.isAccepted(task.id) should be
  }

  it should "have pending tasks" in {
    queueState.hasPendingTasks should be
  }

  it should "return previous task from the queue" in {
    val (task, _) = queueState.nextTask
    task.id should be (TaskId)
  }

  it should "mark in-progress the task if it's started" in {
    queueState = queueState.updated(TaskStarted(TaskId))

    queueState.isInProgress(TaskId) should be
  }

  it should "unmark as in-progress if task times out" in {
    queueState = queueState.updated(TaskTimedOut(TaskId))

    queueState.isInProgress(TaskId) should not
  }

}
