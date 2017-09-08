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

package io.quckoo.cluster.scheduler

import java.util.UUID
import java.time.{Clock, ZonedDateTime, Duration => JavaDuration}

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted}

import cats.syntax.eq._

import io.quckoo._
import io.quckoo.api.TopicTag
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import kamon.trace.Tracer

import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 16/08/15.
  */
object ExecutionDriver {

  final val ShardName      = "ExecutionDriver"
  final val NumberOfShards = 100

  val idExtractor: ShardRegion.ExtractEntityId = {
    case n: Initialize          => (n.planId.toString, n)
    case g: GetExecutionPlan    => (g.planId.toString, g)
    case c: CancelExecutionPlan => (c.planId.toString, c)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case Initialize(_, planId, _, _, _) =>
      (planId.hashCode() % NumberOfShards).toString
    case GetExecutionPlan(planId) =>
      (planId.hashCode() % NumberOfShards).toString
    case CancelExecutionPlan(planId) =>
      (planId.hashCode() % NumberOfShards).toString
  }

  // Only for internal usage from the Scheduler actor
  private[scheduler] final case class Initialize(
      jobId: JobId,
      planId: PlanId,
      spec: JobSpec,
      trigger: Trigger,
      timeout: Option[FiniteDuration]
  ) {
    require(jobId === JobId(spec))
  }

  // Private messages, used for managing the internal lifecycle
  private[scheduler] final case class Initialized(
      jobId: JobId,
      planId: PlanId,
      spec: JobSpec,
      trigger: Trigger,
      timeout: Option[FiniteDuration],
      time: ZonedDateTime
  )

  sealed trait InternalCmd
  private final case class ScheduleTask(task: Task, time: ZonedDateTime) extends InternalCmd
  private case object FinishPlan                                         extends InternalCmd

  trait ExecutionLifecycleFactory {
    def executionLifecycleProps(driverState: DriverState): Props
  }

  object DefaultExecutionLifecycleFactory extends ExecutionLifecycleFactory {
    def executionLifecycleProps(driverState: DriverState): Props =
      ExecutionLifecycle.props(driverState.planId, executionTimeout = driverState.taskTimeout)
  }

  // Public execution driver state
  object DriverState {

    private[scheduler] def apply(initial: Initialized): DriverState =
      DriverState(
        ExecutionPlan(
          initial.jobId,
          initial.planId,
          initial.trigger,
          initial.time
        ),
        initial.spec,
        Vector.empty,
        initial.timeout
      )

  }
  final case class DriverState(
      plan: ExecutionPlan,
      jobSpec: JobSpec,
      completedTasks: Vector[TaskId],
      taskTimeout: Option[FiniteDuration]
  ) {

    val jobId: JobId   = plan.jobId
    val planId: PlanId = plan.planId

    def fired: Boolean = {
      import plan._

      if (lastScheduledTime.isEmpty || currentTask.isEmpty) false
      else {
        val triggerTimeAfterSchedule = for {
          scheduleTime <- lastScheduledTime
          triggerTime  <- lastTriggeredTime
          if triggerTime.isAfter(scheduleTime) || triggerTime.isEqual(scheduleTime)
        } yield triggerTime

        triggerTimeAfterSchedule.isDefined
      }
    }

    def updated(event: SchedulerEvent): DriverState =
      if (plan.finished) this
      else {
        event match {
          case TaskScheduled(`jobId`, `planId`, task, dateTime) if plan.currentTask.isEmpty =>
            copy(
              plan = plan.copy(
                currentTask = Some(task),
                lastScheduledTime = Some(dateTime)
              )
            )

          case TaskTriggered(`jobId`, `planId`, taskId, dateTime)
              if plan.currentTask.map(_.id).contains(taskId) =>
            copy(
              plan = plan.copy(
                lastTriggeredTime = Some(dateTime)
              )
            )

          case TaskCompleted(`jobId`, `planId`, taskId, dateTime, outcome)
              if plan.currentTask.map(_.id).contains(taskId) =>
            copy(
              plan = plan.copy(
                currentTask = None,
                lastExecutionTime = Some(dateTime),
                lastOutcome = Some(outcome)
              ),
              completedTasks = completedTasks :+ taskId
            )

          case ExecutionPlanFinished(`jobId`, `planId`, dateTime) =>
            copy(
              plan = plan.copy(
                currentTask = None,
                finishedTime = Some(dateTime)
              )
            )

          case _ => this
        }
      }

  }

  def props(lifecycleFactory: ExecutionLifecycleFactory)(implicit clock: Clock): Props =
    Props(new ExecutionDriver(lifecycleFactory))

}

class ExecutionDriver(
    lifecycleFactory: ExecutionDriver.ExecutionLifecycleFactory
)(implicit clock: Clock)
    extends PersistentActor with ActorLogging {

  import ExecutionDriver._
  import ShardRegion.Passivate
  import SupervisorStrategy._
  import DistributedPubSubMediator._

  private[this] val mediator = DistributedPubSub(context.system).mediator
  private[this] val triggerDispatcher =
    context.system.dispatchers.lookup("quckoo.trigger-dispatcher")
  private[this] val taskQueue = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "quckoo" / "scheduler" / "queue"
  )

  // Only used to hold the current state of the actor during recovery
  private[this] var stateDuringRecovery: Option[DriverState] = None

  override def persistenceId: String = s"$ShardName-" + self.path.name

  override def preStart(): Unit = {
    log.debug("Execution driver starting with persistence ID: {}", persistenceId)
    mediator ! Subscribe(TopicTag.Registry.name, self)
  }

  override def receiveRecover: Receive = {
    case init: Initialized =>
      log.debug("Execution driver recreated for plan '{}'.", init.planId)
      stateDuringRecovery = Some(DriverState(init))

    case event: SchedulerEvent =>
      log.debug("Execution driver event {} replayed.", event)
      stateDuringRecovery = stateDuringRecovery.map(_.updated(event))

    case RecoveryCompleted =>
      stateDuringRecovery.foreach { st =>
        log.debug("Execution driver recovery finished with state: {}", st.plan)

        if (st.fired) {
          val taskId = st.plan.currentTask.get.id
          log.info("Re-triggering task '{}' after successful recovery.", taskId)
          val lifecycleProps = lifecycleFactory.executionLifecycleProps(st)
          val lifecycle =
            context.watch(context.actorOf(lifecycleProps, taskId.toString))
          context.become(runningExecution(st, lifecycle))
        } else {
          performTransition(st)(scheduleOrFinish(st))
        }
      }
      stateDuringRecovery = None
  }

  override def receiveCommand: Receive = initial()

  private def activatePlan(state: DriverState): Unit = {
    log.info("Activating execution plan '{}'.", state.planId)
    persist(ExecutionPlanStarted(state.jobId, state.planId, ZonedDateTime.now(clock))) { event =>
      mediator ! Publish(TopicTag.Scheduler.name, event)
      performTransition(state)(scheduleOrFinish(state))
    }
  }

  /**
    * Initial state for the ExecutionDriver as it needs to wait for acknowledge from
    * the distributed pub/sub and an initial New command
    */
  private def initial(subscribed: Boolean = false, state: Option[DriverState] = None): Receive = {
    case SubscribeAck(Subscribe(topic, _, `self`)) if topic == TopicTag.Registry.name =>
      if (state.isDefined) {
        activatePlan(state.get)
      } else {
        context.become(initial(subscribed = true, state))
      }

    case GetExecutionPlan(_) => stash()

    case init: Initialize =>
      val initialized = Initialized(
        init.jobId,
        init.planId,
        init.spec,
        init.trigger,
        init.timeout,
        ZonedDateTime.now(clock)
      )
      persist(initialized) { evt =>
        val st = DriverState(evt)
        log.debug("Execution plan '{}' initialized. Starting...", st.planId)

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
      log.info("Job '{}' has been disabled, finishing execution plan '{}'.", id, state.planId)
      self ! FinishPlan
      context become shuttingDown(state)

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case ScheduleTask(task, time) =>
      def createTrigger(task: Task,
                        planId: PlanId,
                        lifecycle: ActorRef,
                        when: ZonedDateTime): Cancellable = {
        val delay = {
          val now = ZonedDateTime.now(clock)
          if (when.isBefore(now) || when.isEqual(now)) 0 millis
          else JavaDuration.between(now, when).toMillis millis
        }

        implicit val dispatcher = triggerDispatcher
        log.debug("Task '{}' in plan '{}' will be triggered after {}", task.id, planId, delay)
        context.system.scheduler
          .scheduleOnce(delay, lifecycle, ExecutionLifecycle.Awake(task, taskQueue))
      }

      // Instantiate a new execution lifecycle
      val lifecycle = Tracer.withNewContext(s"execution-${task.id}") {
        val lifecycleProps = lifecycleFactory.executionLifecycleProps(state)
        context.watch(context.actorOf(lifecycleProps, task.id.toString))
      }

      // Create a trigger to fire the task
      val trigger = createTrigger(task, state.planId, lifecycle, time)
      persist(TaskScheduled(state.jobId, state.planId, task, ZonedDateTime.now(clock))) { event =>
        unstashAll()

        mediator ! Publish(TopicTag.Scheduler.name, event)
        context.become(runningExecution(state.updated(event), lifecycle, Some(trigger)))
      }

    case _ => stash()
  }

  /**
    * Running execution, waits for the execution to notify its completeness and
    * accepts cancellation (if possible)
    */
  private def runningExecution(state: DriverState,
                               lifecycle: ActorRef,
                               trigger: Option[Cancellable] = None): Receive = {
    case ExecutionLifecycle.Triggered(task) =>
      log.debug("Trigger for task '{}' has successfully fired.", task.id)
      persist(TaskTriggered(state.jobId, state.planId, task.id, ZonedDateTime.now(clock))) {
        event =>
          mediator ! Publish(TopicTag.Scheduler.name, event)
          context.become(runningExecution(state.updated(event), lifecycle, None))
      }

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case CancelExecutionPlan(_) =>
      if (trigger.isDefined) {
        log.debug("Cancelling trigger for execution plan '{}'.", state.planId)
        trigger.foreach(_.cancel())
      }
      lifecycle ! ExecutionLifecycle.Cancel(TaskExecution.Reason.UserRequest)
      context.become(runningExecution(state, context.unwatch(lifecycle)))

    case ExecutionLifecycle.Result(outcome) =>
      state.plan.currentTask.foreach { task =>
        persist(
          TaskCompleted(state.jobId, state.planId, task.id, ZonedDateTime.now(clock), outcome)
        ) { event =>
          log.debug("Task '{}' finished with outcome: {}", task.id, outcome)
          mediator ! Publish(TopicTag.Scheduler.name, event)

          val newState = state.updated(event)
          context.unwatch(lifecycle)

          performTransition(newState)(nextCommand(newState, task.id, outcome))
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
    case FinishPlan if !state.plan.finished =>
      log.info("Finishing execution plan '{}'.", state.planId)
      persist(ExecutionPlanFinished(state.jobId, state.planId, ZonedDateTime.now(clock))) { event =>
        mediator ! Publish(TopicTag.Scheduler.name, event)
        mediator ! Unsubscribe(TopicTag.Registry.name, self)
        context.become(shuttingDown(state.updated(event)))
      }

    case FinishPlan =>
      mediator ! Unsubscribe(TopicTag.Registry.name, self)

    case UnsubscribeAck(Unsubscribe(topic, _, `self`)) if topic == TopicTag.Registry.name =>
      context.parent ! Passivate(stopMessage = PoisonPill)

    case GetExecutionPlan(_) =>
      sender() ! state.plan

    case SubscribeAck(Subscribe(topic, _, `self`)) if topic == TopicTag.Registry.name =>
    // May happen when the driver was reloaded after a DB recovery but the plan has already been finished

    case msg: Any =>
      log.warning(
        "Execution plan '{}' has finished. Ignoring message " +
          "{} while shutting down.",
        state.planId,
        msg
      )
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException           => Stop
    case cause: Exception =>
      log.error(cause, "Error thrown from the execution state manager")
      Restart
  }

  private[this] def performTransition(state: DriverState)(cmd: InternalCmd): Unit = {
    unstashAll()
    self ! cmd

    val behaviour = cmd match {
      case FinishPlan => shuttingDown(state)
      case _          => ready(state)
    }

    context become behaviour
  }

  private def scheduleOrFinish(state: DriverState): InternalCmd =
    if (state.plan.finished) FinishPlan
    else {
      def createTask = Task(
        TaskId(UUID.randomUUID()),
        state.jobSpec.jobPackage
      )
      val task = state.plan.currentTask.getOrElse(createTask)
      state.plan.nextExecutionTime.map { when =>
        ScheduleTask(task, when)
      } getOrElse FinishPlan
    }

  private def nextCommand(state: DriverState,
                          lastTaskId: TaskId,
                          outcome: TaskExecution.Outcome): InternalCmd =
    if (state.plan.trigger.isRecurring) {
      outcome match {
        case TaskExecution.Outcome.Success =>
          scheduleOrFinish(state)

        case TaskExecution.Outcome.Failure(cause) =>
          if (shouldRetry(cause)) {
            // TODO improve retry process
            scheduleOrFinish(state)
          } else {
            log.warning("Failed task '{}' won't be retried.", lastTaskId)
            FinishPlan
          }

        case _ => FinishPlan
      }
    } else FinishPlan

  private def shouldRetry(cause: QuckooError): Boolean = cause match {
    case ex: ExceptionThrown => false
    case _                   => true
  }

}
