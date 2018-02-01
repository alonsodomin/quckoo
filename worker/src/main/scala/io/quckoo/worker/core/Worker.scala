/*
 * Copyright 2015 A. Alonso Dominguez
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

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.cluster.client.ClusterClient.SendToAll

import cats.effect.IO

import io.quckoo.{ExceptionThrown, NodeId, Task, TaskId}
import io.quckoo.cluster.protocol._
import io.quckoo.resolver.Resolver

import kamon.Kamon
import kamon.metric.Counter

import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 05/07/15.
  */
object Worker {

  final val DefaultRegisterFrequency: FiniteDuration = 10 seconds
  final val DefaultQueueAckTimeout: FiniteDuration   = 5 seconds

  protected[worker] final val SchedulerPath = "/user/quckoo/scheduler"

  def props(clusterClient: ActorRef,
            resolver: Resolver[IO],
            taskExecutorProvider: TaskExecutorProvider,
            registerInterval: FiniteDuration = DefaultRegisterFrequency,
            queueAckTimeout: FiniteDuration = DefaultQueueAckTimeout): Props =
    Props(
      new Worker(
        clusterClient,
        resolver,
        taskExecutorProvider,
        registerInterval,
        queueAckTimeout
      )
    )

}

class Worker private (clusterClient: ActorRef,
                      resolver: Resolver[IO],
                      taskExecutorProvider: TaskExecutorProvider,
                      registerInterval: FiniteDuration,
                      queueAckTimeout: FiniteDuration)
    extends Actor with ActorLogging { myself =>

  import Worker._
  import context.dispatcher

  val workerId = NodeId(UUID.randomUUID())

  private[this] val registerTask = context.system.scheduler.schedule(
    0 seconds,
    registerInterval,
    clusterClient,
    SendToAll(SchedulerPath, RegisterWorker(workerId))
  )

  private[this] val workerContext = new WorkerContext {
    val resolver: Resolver[IO] = myself.resolver
  }
  private[this] var executor: Option[ActorRef] = None

  private[this] var currentTaskId: Option[TaskId] = None

  private[this] val successfulTasksCounter: Counter =
    Kamon.counter("successful-tasks")
  private[this] val failedTasksCounter: Counter =
    Kamon.counter("failed-tasks")

  def taskId: TaskId = currentTaskId match {
    case Some(id) => id
    case None     => throw new IllegalStateException("Not working")
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive: Receive = idle

  private[this] def idle: Receive = {
    case TaskReady =>
      log.info("Requesting task to master.")
      sendToMaster(RequestTask(workerId))

    case task: Task =>
      log.info("Received task for execution {}", task.id)
      currentTaskId = Some(task.id)
      //Tracer.withNewContext(s"task-${task.id}") {
      executor = Some(taskExecutorProvider.executorFor(workerContext, task))
        .map(context.watch)
      executor.foreach(_ ! TaskExecutor.Run)
      //}
      context.become(working(task))
  }

  private[this] def working(task: Task): Receive = {
    case TaskExecutor.Completed(result) =>
      log.info("Task execution has completed. Result {}.", result)
      sendToMaster(TaskDone(workerId, task.id, result))
      stopExecutor()
      successfulTasksCounter.increment()
      context.setReceiveTimeout(queueAckTimeout)
      context.become(waitForTaskDoneAck(result))

    case TaskExecutor.Failed(reason) =>
      sendToMaster(TaskFailed(workerId, task.id, reason))
      stopExecutor()
      failedTasksCounter.increment()
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case _: Task =>
      log.info("Yikes. The task queue has sent me another another task while I'm busy.")
  }

  private[this] def waitForTaskDoneAck(result: Any): Receive = {
    case TaskDoneAck(id) if id == taskId =>
      sendToMaster(RequestTask(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.warning(
        "Didn't receive any ack from task queue in the last {}, retrying",
        queueAckTimeout
      )
      sendToMaster(TaskDone(workerId, taskId, result))
      context.setReceiveTimeout(queueAckTimeout)
  }

  private[this] def sendToMaster(msg: Any): Unit =
    clusterClient ! SendToAll(SchedulerPath, msg)

  private[this] def stopExecutor(): Unit = {
    executor.map(context.unwatch).foreach(context.stop)
    executor = None
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException           => Stop
    case cause: Exception =>
      stopExecutor()
      currentTaskId.foreach { taskId =>
        sendToMaster(TaskFailed(workerId, taskId, ExceptionThrown.from(cause)))
      }
      context.become(idle)
      Stop
  }

  override def unhandled(message: Any): Unit = message match {
    case Terminated(actorRef) if executor.contains(actorRef) =>
      // Unexpected executor termination
      context.stop(self)

    case TaskReady =>
    case _         => super.unhandled(message)
  }

}
