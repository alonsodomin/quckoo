package io.chronos.worker

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.cluster.client.ClusterClient.SendToAll
import io.chronos.cluster.{Task, WorkerProtocol}
import io.chronos.id.TaskId
import io.chronos.path

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Worker {

  def props(clusterClient: ActorRef, jobExecutorProps: Props, registerInterval: FiniteDuration = 10 seconds): Props =
    Props(classOf[Worker], clusterClient, jobExecutorProps, registerInterval)

}

class Worker(clusterClient: ActorRef, jobExecutorProps: Props, registerInterval: FiniteDuration) extends Actor with ActorLogging {
  import WorkerProtocol._
  import context.dispatcher

  val workerId = UUID.randomUUID()
  
  val registerTask = context.system.scheduler.schedule(
    0.seconds, registerInterval, clusterClient,
    SendToAll(path.Scheduler, RegisterWorker(workerId))
  )

  val jobExecutor = context.watch(context.actorOf(jobExecutorProps, "executor"))
  
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
      sendToMaster(RequestTask(workerId))

    case task: Task =>
      log.info("Received task for execution {}", task.id)
      currentTaskId = Some(task.id)
      jobExecutor ! JobExecutor.Execute(task)
      context.become(working)
  }

  def working: Receive = {
    case JobExecutor.Completed(taskId, result) =>
      log.info("Task execution has completed. Result {}.", result)
      sendToMaster(TaskDone(workerId, taskId, result))
      context.setReceiveTimeout(5 seconds)
      context.become(waitForWorkDoneAck(result))

    case JobExecutor.Failed(taskId, reason) =>
      sendToMaster(TaskFailed(workerId, taskId, reason))
      context.setReceiveTimeout(5 seconds)
      context.become(idle)

    case _: Task =>
      log.info("Yikes. Master told me to do task, while I'm working.")
  }

  def waitForWorkDoneAck(result: Any): Receive = {
    case TaskDoneAck(id) if id == taskId =>
      sendToMaster(RequestTask(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.info("No ack from master, retrying")
      sendToMaster(TaskDone(workerId, taskId, result))
  }

  private def sendToMaster(msg: Any): Unit = {
    clusterClient ! SendToAll(path.Scheduler, msg)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case cause: Exception =>
      currentTaskId.foreach {
        taskId => sendToMaster(TaskFailed(workerId, taskId, Right(cause)))
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