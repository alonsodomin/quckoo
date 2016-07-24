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

package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}

import io.quckoo.cluster.topics
import io.quckoo.fault.{ExceptionThrown, Fault}
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.time.{DateTime, TimeSource}
import io.quckoo.{ExecutionPlan, JobSpec, Task, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionDriver {

  final val ShardName      = "ExecutionDriver"
  final val NumberOfShards = 100

  val idExtractor: ShardRegion.ExtractEntityId = {
    case n: New              => (n.planId.toString, n)
    case g: GetExecutionPlan => (g.planId.toString, g)
    case c: CancelExecutionPlan       => (c.planId.toString, c)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case New(_, _, planId, _, _)  => (planId.hashCode() % NumberOfShards).toString
    case GetExecutionPlan(planId) => (planId.hashCode() % NumberOfShards).toString
    case CancelExecutionPlan(planId)       => (planId.hashCode() % NumberOfShards).toString
  }

  // Only for internal usage from the Scheduler actor
  private[scheduler] case class New(
      jobId: JobId, spec: JobSpec, planId: PlanId,
      trigger: Trigger, executionProps: Props
  )

  // Private messages, used for managing the internal lifecycle
  private[scheduler] final case class Created(
      jobId: JobId, spec: JobSpec, planId: PlanId,
      trigger: Trigger, executionProps: Props,
      time: DateTime
  )
  private final case class ScheduleTask(task: Task, time: DateTime)
  private case object FinishPlan

  // Public execution driver state
  object DriverState {
    private[scheduler] def apply(created: Created)(implicit timeSource: TimeSource): DriverState = DriverState(
      ExecutionPlan(created.jobId, created.planId, created.trigger, created.time),
      created.spec,
      created.executionProps,
      Vector.empty
    )
  }
  final case class DriverState(
      plan: ExecutionPlan,
      jobSpec: JobSpec,
      executionProps: Props,
      completedTasks: Vector[TaskId]
  )(implicit timeSource: TimeSource) {

    val jobId = plan.jobId
    val planId = plan.planId

    def fired: Boolean = {
      import plan._

      if (lastScheduledTime.isEmpty || currentTaskId.isEmpty) false
      else {
        val triggerTimeAfterSchedule = for {
          scheduleTime  <- lastScheduledTime
          triggerTime   <- lastTriggeredTime if triggerTime.isAfter(scheduleTime)
        } yield triggerTime

        triggerTimeAfterSchedule.isDefined
      }
    }

    def updated(event: SchedulerEvent): DriverState = {
      if (plan.finished) this
      else {
        event match {
          case TaskScheduled(`jobId`, `planId`, taskId) if plan.currentTaskId.isEmpty =>
            copy(plan = plan.copy(
              currentTaskId = Some(taskId),
              lastScheduledTime = Some(timeSource.currentDateTime)
            ))

          case TaskTriggered(`jobId`, `planId`, taskId) if plan.currentTaskId.contains(taskId) =>
            copy(plan = plan.copy(
              lastTriggeredTime = Some(timeSource.currentDateTime)
            ))

          case TaskCompleted(`jobId`, `planId`, taskId, outcome) if plan.currentTaskId.contains(taskId) =>
            copy(plan = plan.copy(
              currentTaskId = None,
              lastExecutionTime = Some(timeSource.currentDateTime),
              lastOutcome = outcome),
              completedTasks = completedTasks :+ taskId
            )

          case ExecutionPlanFinished(`jobId`, `planId`) =>
            copy(plan = plan.copy(
              currentTaskId = None,
              finishedTime = Some(timeSource.currentDateTime)
            ))

          case _ => this
        }
      }
    }

  }

  def props(implicit timeSource: TimeSource) =
    Props(classOf[ExecutionDriver], timeSource)

}

class ExecutionDriver(implicit timeSource: TimeSource)
    extends PersistentActor with ActorLogging {

  import ExecutionDriver._
  import ShardRegion.Passivate
  import SupervisorStrategy._
  import DistributedPubSubMediator._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] val triggerDispatcher = context.system.dispatchers.lookup("quckoo.trigger-dispatcher")
  private[this] val taskQueue = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "quckoo" / "scheduler" / "queue"
  )

  // Only used to hold the current state of the actor during recovery
  private[this] var stateDuringRecovery: Option[DriverState] = None

  override def persistenceId = s"$ShardName-" + self.path.name

  override def preStart(): Unit =
    mediator ! Subscribe(topics.Registry, self)

  override def receiveRecover: Receive = {
    case create: Created =>
      log.debug("Execution driver recreated for plan: {}", create.planId)
      stateDuringRecovery = Some(DriverState(create))

    case event: SchedulerEvent =>
      log.debug("Execution driver event replayed. event={}", event)
      stateDuringRecovery = stateDuringRecovery.map(_.updated(event))

    case RecoveryCompleted =>
      stateDuringRecovery.foreach { st =>
        log.debug("Execution driver recovery finished. state={}")

        if (st.fired) {
          val lifecycle = context.watch(context.actorOf(st.executionProps, st.plan.currentTaskId.get.toString))
          context.become(runningExecution(st, lifecycle))
        } else {
          scheduleOrFinish(st) match {
            case schedule: ScheduleTask =>
              self ! schedule
              context.become(ready(st))

            case _ =>
              self ! FinishPlan
              context.become(shuttingDown(st))
          }
        }
      }
      stateDuringRecovery = None
  }

  override def receiveCommand: Receive = initial()

  private def activatePlan(state: DriverState): Unit = {
    log.info("Activating execution plan. planId={}", state.planId)
    persist(ExecutionPlanStarted(state.jobId, state.planId)) { event =>
      mediator ! Publish(topics.Scheduler, event)
      scheduleOrFinish(state) match {
        case FinishPlan =>
          self ! FinishPlan
          context become shuttingDown(state)

        case other =>
          self ! other
          unstashAll()
          context.become(ready(state))
      }
    }
  }

  /**
   * Initial state for the ExecutionDriver as it needs to wait for acknowledge from
   * the distributed pub/sub and an initial New command
   */
  private def initial(subscribed: Boolean = false, state: Option[DriverState] = None): Receive = {
    case SubscribeAck(Subscribe(topic, _, `self`)) if topic == topics.Registry =>
      if (state.isDefined) {
        activatePlan(state.get)
      } else {
        context.become(initial(subscribed = true, state))
      }

    case GetExecutionPlan(_) => stash()

    case cmd: New =>
      val created = Created(cmd.jobId, cmd.spec, cmd.planId, cmd.trigger,
          cmd.executionProps, timeSource.currentDateTime)
      persist(created) { evt =>
        val st = DriverState(evt)
        log.debug("Creating new execution plan. planId={}", st.planId)

        if (subscribed) {
          activatePlan(st)
        } else {
          context.become(initial(subscribed, state = Some(st)))
        }
      }
  }

  /**
   * Ready state, it accepts scheduling new tasks or gracefully finishing the execution plan
   */
  private def ready(state: DriverState): Receive = {
    case JobDisabled(id) if id == state.jobId =>
      log.info("Job has been disabled, finishing execution plan. jobId={}, planId={}", id, state.planId)
      self ! FinishPlan
      context become shuttingDown(state)

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case ScheduleTask(task, time) =>
      def createTrigger(task: Task, planId: PlanId, lifecycle: ActorRef, when: DateTime): Cancellable = {
        val delay = {
          val now = timeSource.currentDateTime
          if (when.isBefore(now) || when.isEqual(now)) 0 millis
          else (when - now).toMillis millis
        }

        implicit val dispatcher = triggerDispatcher
        log.debug("Task {} in plan {} will be triggered after {}", task.id, planId, delay)
        context.system.scheduler.scheduleOnce(delay, lifecycle, Execution.Enqueue(task, taskQueue))
      }

      // Instantiate a new execution lifecycle
      val lifecycle = context.watch(context.actorOf(state.executionProps, task.id.toString))

      // Create a trigger to fire the task
      val trigger = createTrigger(task, state.planId, lifecycle, time)
      persist(TaskScheduled(state.jobId, state.planId, task.id)) { event =>
        unstashAll()

        mediator ! Publish(topics.Scheduler, event)
        context.become(runningExecution(state.updated(event), lifecycle, Some(trigger)))
      }

    case _ => stash()
  }

  /**
   * Running execution, waits for the execution to notify its completeness and
   * accepts cancellation (if possible)
   */
  private def runningExecution(state: DriverState, lifecycle: ActorRef,
                               trigger: Option[Cancellable] = None): Receive = {
    case Execution.Triggered(task) =>
      log.debug("Trigger for task {} has successfully fired.", task.id)
      persist(TaskTriggered(state.jobId, state.planId, task.id)) { event =>
        context.become(runningExecution(state.updated(event), lifecycle, None))
      }

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case CancelExecutionPlan(_) =>
      if (trigger.isDefined) {
        log.debug("Cancelling trigger for execution plan. planId={}", state.planId)
        trigger.foreach(_.cancel())
      }
      lifecycle ! Execution.Cancel(Task.UserRequest)
      context.unwatch(lifecycle)
      context.become(runningExecution(state, lifecycle))

    case Execution.Result(outcome) =>
      state.plan.currentTaskId.foreach { taskId =>
        persist(TaskCompleted(state.jobId, state.planId, taskId, outcome)) { event =>
          log.debug("Task finished. taskId={}, outcome={}", taskId, outcome)
          mediator ! Publish(topics.Scheduler, event)

          val newState = state.updated(event)
          context.unwatch(lifecycle)

          unstashAll()
          proceedNext(newState, taskId, outcome)
        }
      }

    case ScheduleTask(_, _) =>
      log.warning("Received a `ScheduleTask` command while an execution is running!")

    case _ => stash()
  }

  /**
   * State prior to fully stopping the actor.
   */
  private def shuttingDown(state: DriverState): Receive = {
    case FinishPlan =>
      log.info("Finishing execution plan. planId={}", state.planId)
      persist(ExecutionPlanFinished(state.jobId, state.planId)) { event =>
        mediator ! Publish(topics.Scheduler, event)
        mediator ! Unsubscribe(topics.Registry, self)
        context.become(shuttingDown(state.updated(event)))
      }

    case UnsubscribeAck(Unsubscribe(topic, _, `self`)) if topic == topics.Registry =>
      context.parent ! Passivate(stopMessage = PoisonPill)

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case msg: Any =>
      log.warning("Execution plan '{}' has finished. Ignoring message " +
        "{} while shutting down.", state.planId, msg)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case cause: Exception =>
      log.error(cause, "Error thrown from the execution state manager")
      Restart
  }

  private def scheduleOrFinish(state: DriverState) = {
    val task = {
      val taskId = state.plan.currentTaskId.getOrElse(UUID.randomUUID())
      Task(taskId, state.jobSpec.artifactId, state.jobSpec.jobClass)
    }
    state.plan.nextExecutionTime.map(when => ScheduleTask(task, when)).getOrElse(FinishPlan)
  }

  private def proceedNext(state: DriverState, lastTaskId: TaskId, outcome: Task.Outcome): Unit = {
    def nextCommand = {
      if (state.plan.trigger.isRecurring) {
        outcome match {
          case Task.Success =>
            scheduleOrFinish(state)

          case Task.Failure(cause) =>
            if (shouldRetry(cause)) {
              // TODO improve retry process
              scheduleOrFinish(state)
            } else {
              log.warning("Failed task {} won't be retried.", lastTaskId)
              FinishPlan
            }

          case _ => FinishPlan
        }
      } else FinishPlan
    }

    nextCommand match {
      case FinishPlan =>
        self ! FinishPlan
        context become shuttingDown(state)

      case cmd: ScheduleTask =>
        self ! cmd
        context become ready(state)
    }
  }

  private def shouldRetry(cause: Fault): Boolean = cause match {
    case ex: ExceptionThrown => false
    case _                   => true
  }

}
