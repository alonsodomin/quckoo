package io.kairos.worker

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.cluster.client.ClusterClient.SendToAll
import io.kairos.Task
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.fault.ExceptionThrown
import io.kairos.id.TaskId

import scala.concurrent.duration._
import scalaz.NonEmptyList

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Worker {

  final val DefaultRegisterFrequency = 10 seconds
  final val DefaultQueueAckTimeout = 5 seconds

  protected[worker] final val SchedulerPath = "/user/kairos/scheduler"

  def props(clusterClient: ActorRef, jobExecutorProps: Props,
            registerInterval: FiniteDuration = DefaultRegisterFrequency,
            queueAckTimeout: FiniteDuration = DefaultQueueAckTimeout): Props =
    Props(classOf[Worker], clusterClient, jobExecutorProps, registerInterval, queueAckTimeout)

}

class Worker(clusterClient: ActorRef,
             jobExecutorProps: Props,
             registerInterval: FiniteDuration,
             queueAckTimeout: FiniteDuration)
    extends Actor with ActorLogging {

  import Worker._
  import WorkerProtocol._
  import context.dispatcher

  val workerId = UUID.randomUUID()
  
  val registerTask = context.system.scheduler.schedule(
    0 seconds, registerInterval, clusterClient,
    SendToAll(SchedulerPath, RegisterWorker(workerId))
  )

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
      jobExecutor ! JobExecutor.Execute(task)
      context.become(working)
  }

  def working: Receive = {
    case JobExecutor.Completed(result) =>
      log.info("Task execution has completed. Result {}.", result)
      sendToQueue(TaskDone(workerId, taskId, result))
      context.setReceiveTimeout(queueAckTimeout)
      context.become(waitForTaskDoneAck(result))

    case JobExecutor.Failed(reason) =>
      sendToQueue(TaskFailed(workerId, taskId, reason))
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
        taskId => sendToQueue(TaskFailed(workerId, taskId, NonEmptyList(ExceptionThrown(cause))))
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