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

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.cluster.client.ClusterClient.SendToAll

import io.quckoo.Task
import io.quckoo.cluster.protocol._
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.TaskId
import io.quckoo.resolver.Resolver

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Worker {

  final val DefaultRegisterFrequency = 10 seconds
  final val DefaultQueueAckTimeout = 5 seconds

  protected[worker] final val SchedulerPath = "/user/quckoo/scheduler"

  def props(clusterClient: ActorRef, resolverProps: Props, jobExecutorProps: Props,
            registerInterval: FiniteDuration = DefaultRegisterFrequency,
            queueAckTimeout: FiniteDuration = DefaultQueueAckTimeout): Props =
    Props(classOf[Worker], clusterClient, resolverProps, jobExecutorProps, registerInterval, queueAckTimeout)

}

class Worker(clusterClient: ActorRef,
             resolverProps: Props,
             jobExecutorProps: Props,
             registerInterval: FiniteDuration,
             queueAckTimeout: FiniteDuration)
    extends Actor with ActorLogging {

  import Worker._
  import context.dispatcher

  val workerId = UUID.randomUUID()
  
  val registerTask = context.system.scheduler.schedule(
    0 seconds, registerInterval, clusterClient,
    SendToAll(SchedulerPath, RegisterWorker(workerId))
  )

  private val resolver = context.watch(context.actorOf(resolverProps, "resolver"))
  private val jobExecutor = context.watch(context.actorOf(jobExecutorProps, "executor"))
  
  private var currentTaskId: Option[TaskId] = None

  def taskId: TaskId = currentTaskId match {
    case Some(id) => id
    case None     => throw new IllegalStateException("Not working")
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive = idle

  def idle: Receive = {
    case TaskReady =>
      log.info("Requesting task to master.")
      sendToQueue(RequestTask(workerId))

    case task: Task =>
      log.info("Received task for execution {}", task.id)
      currentTaskId = Some(task.id)
      resolver ! Resolver.Download(task.artifactId)
      context.become(working(task))
  }

  def working(task: Task): Receive = {
    case Resolver.ArtifactResolved(artifact) =>
      jobExecutor ! JobExecutor.Execute(task, artifact)

    case Resolver.ResolutionFailed(_, cause) =>
      sendToQueue(TaskFailed(workerId, task.id, cause))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case JobExecutor.Completed(result) =>
      log.info("Task execution has completed. Result {}.", result)
      sendToQueue(TaskDone(workerId, task.id, result))
      context.setReceiveTimeout(queueAckTimeout)
      context.become(waitForTaskDoneAck(result))

    case JobExecutor.Failed(reason) =>
      sendToQueue(TaskFailed(workerId, task.id, reason))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case _: Task =>
      log.info("Yikes. The task queue has sent me another another task while I'm busy.")
  }

  def waitForTaskDoneAck(result: Any): Receive = {
    case TaskDoneAck(id) if id == taskId =>
      sendToQueue(RequestTask(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.warning("Didn't receive any ack from task queue in the last {}, retrying", queueAckTimeout)
      sendToQueue(TaskDone(workerId, taskId, result))
      context.setReceiveTimeout(queueAckTimeout)
  }

  private def sendToQueue(msg: Any): Unit = {
    clusterClient ! SendToAll(SchedulerPath, msg)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case cause: Exception =>
      currentTaskId.foreach {
        taskId => sendToQueue(TaskFailed(workerId, taskId, ExceptionThrown.from(cause)))
      }
      context.become(idle)
      Restart
  }

  override def unhandled(message: Any): Unit = message match {
    case Terminated(`jobExecutor`) => context.stop(self)
    case TaskReady =>
    case _ => super.unhandled(message)
  }

}
