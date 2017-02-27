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
import akka.testkit._

import io.quckoo._
import io.quckoo.cluster.topics
import io.quckoo.id.{ArtifactId, JobId, PlanId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.testkit.{ImplicitClock, QuckooActorClusterSuite}

import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionDriverSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", Some("foo desc"), JobPackage.jar(TestArtifactId, "com.example.Job"))
  final val TestJobId = JobId(TestJobSpec)

}

class ExecutionDriverSpec extends QuckooActorClusterSuite("ExecutionDriverSpec")
    with ImplicitSender with ImplicitClock with BeforeAndAfter with Inside {

  import ExecutionDriver._
  import ExecutionDriverSpec._

  val mediator = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_) => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  val eventListener = TestProbe()

  before {
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, eventListener.ref)
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, eventListener.ref)
  }

  def executionDriverProps(lifecycleFactory: ExecutionLifecycleFactory): Props =
    ExecutionDriver.props(lifecycleFactory).withDispatcher("akka.actor.default-dispatcher")

  "An execution driver with a recurring trigger that eventually gets disabled" should  {
    val trigger = Trigger.Every(50 millis)
    val lifecycleProbe = TestProbe()
    val lifecycleProps = TestActors.forwardActorProps(lifecycleProbe.ref)
    val lifecycleFactory: ExecutionLifecycleFactory = (_: DriverState) => lifecycleProps

    val planId = PlanId(UUID.randomUUID())
    val driverName = "executionDriverWithRecurringTrigger"
    val executionDriver = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
    watch(executionDriver)

    "create an execution from a job specification" in {
      executionDriver ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      within(50 millis) {
        lifecycleProbe.expectMsgType[ExecutionLifecycle.Awake]
      }
    }

    "re-schedule an execution once it finishes" in {
      val successOutcome = TaskExecution.Success
      lifecycleProbe.reply(ExecutionLifecycle.Result(successOutcome))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.planId shouldBe planId
      completedMsg.outcome shouldBe successOutcome

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId shouldBe TestJobId
      scheduledMsg.planId shouldBe planId

      within(100 millis) {
        lifecycleProbe.expectMsgType[ExecutionLifecycle.Awake]
      }
    }

    "not re-schedule an execution after the job is disabled" in {
      mediator ! DistributedPubSubMediator.Publish(topics.Registry, JobDisabled(TestJobId))
      // Complete the execution so the driver can come back to ready state
      lifecycleProbe.reply(ExecutionLifecycle.Result(TaskExecution.Success))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.outcome shouldBe TaskExecution.Success

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId shouldBe TestJobId
      finishedMsg.planId shouldBe planId

      lifecycleProbe.expectNoMsg()

      executionDriver ! PoisonPill
      expectTerminated(executionDriver)
    }

    "self-passivate if restarted again" in {
      // Using the same actor name here to force a re-encarnation
      val reencarnatedDriver = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
      watch(reencarnatedDriver)

      reencarnatedDriver ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      eventListener.expectNoMsg()
      reencarnatedDriver ! PoisonPill

      expectTerminated(reencarnatedDriver)
    }
  }

  "An execution driver with a recurring trigger that eventually gets cancelled" should {
    val trigger = Trigger.Every(50 millis)
    val lifecycleProbe = TestProbe()
    val lifecycleProps = TestActors.forwardActorProps(lifecycleProbe.ref)
    val lifecycleFactory: ExecutionLifecycleFactory = (_: DriverState) => lifecycleProps

    val planId = PlanId(UUID.randomUUID())
    val driverName = "executionDriverWithRecurringTriggerAndCancellation"
    val executionDriver = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
    watch(executionDriver)

    "create an execution from a job specification" in {
      executionDriver ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      within(50 millis) {
        lifecycleProbe.expectMsgType[ExecutionLifecycle.Awake]
      }
    }

    "instruct the execution to immediately stop when it's cancelled" in {
      executionDriver ! CancelExecutionPlan(planId)

      val cancelMsg = lifecycleProbe.expectMsgType[ExecutionLifecycle.Cancel]
      cancelMsg.reason shouldBe TaskExecution.UserRequest

      lifecycleProbe.reply(ExecutionLifecycle.Result(TaskExecution.Interrupted(TaskExecution.UserRequest)))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId shouldBe TestJobId
      completedMsg.outcome shouldBe TaskExecution.Interrupted(TaskExecution.UserRequest)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId shouldBe TestJobId
      finishedMsg.planId shouldBe planId

      lifecycleProbe.expectNoMsg()

      executionDriver ! PoisonPill
      expectTerminated(executionDriver)
    }

    "self-passivate if restarted again" in {
      // Using the same actor name here to force a re-encarnation
      val reencarnatedDriver = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
      watch(reencarnatedDriver)

      reencarnatedDriver ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      eventListener.expectNoMsg()
      reencarnatedDriver ! PoisonPill

      expectTerminated(reencarnatedDriver)
    }
  }

  "An execution driver with non recurring trigger" should {
    val trigger = Trigger.Immediate
    val lifecycleProbe = TestProbe()
    val lifecycleProps = TestActors.forwardActorProps(lifecycleProbe.ref)
    val lifecycleFactory: ExecutionLifecycleFactory = (_: DriverState) => lifecycleProps

    val planId = PlanId(UUID.randomUUID())
    val executionPlan = TestActorRef(executionDriverProps(lifecycleFactory), self, "executionPlanWithOneShotTrigger")
    watch(executionPlan)

    "create an execution from a job specification" in {
      executionPlan ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (planId)

      lifecycleProbe.expectMsgType[ExecutionLifecycle.Awake]
    }

    "terminate once the execution finishes" in {
      val successOutcome = TaskExecution.Success
      lifecycleProbe.send(executionPlan, ExecutionLifecycle.Result(successOutcome))

      val completedMsg = eventListener.expectMsgType[TaskCompleted]
      completedMsg.jobId should be (TestJobId)
      completedMsg.planId should be (planId)
      completedMsg.outcome should be (successOutcome)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId should be (TestJobId)
      finishedMsg.planId should be (planId)

      lifecycleProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }
  }

  "An execution driver that dies after firing the task" should {
    val trigger = Trigger.Immediate
    val lifecycleProbe = TestProbe()
    val lifecycleProps = TestActors.forwardActorProps(lifecycleProbe.ref)
    val lifecycleFactory: ExecutionLifecycleFactory = (_: DriverState) => lifecycleProps

    val planId = PlanId(UUID.randomUUID())
    val driverName = "executionDriverThatDiesAfterFire"
    val executionPlan = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
    watch(executionPlan)

    var task: Task = null

    "create an execution from a job specification before dying" in {
      executionPlan ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId shouldBe TestJobId
      startedMsg.planId shouldBe planId

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId shouldBe TestJobId
      scheduledMsg.planId shouldBe planId

      lifecycleProbe.expectMsgType[ExecutionLifecycle.Awake]
      lifecycleProbe.reply(ExecutionLifecycle.Triggered(scheduledMsg.task))

      val triggeredMsg = eventListener.expectMsgType[TaskTriggered]
      triggeredMsg.jobId shouldBe TestJobId
      triggeredMsg.planId shouldBe planId
      triggeredMsg.taskId shouldBe scheduledMsg.task.id

      task = scheduledMsg.task

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }

    "restore the execution after re-encarnating" in {
      val reencarnatedDriver = system.actorOf(executionDriverProps(lifecycleFactory), driverName)
      watch(reencarnatedDriver)

      lifecycleProbe.send(reencarnatedDriver, ExecutionLifecycle.Triggered(task))

      val triggeredMsg = eventListener.expectMsgType[TaskTriggered]
      triggeredMsg.jobId shouldBe TestJobId
      triggeredMsg.planId shouldBe planId
      triggeredMsg.taskId shouldBe task.id
    }
  }

  "An execution plan that never gets fired" should {
    val trigger = Trigger.At(currentDateTime.minusHours(1), graceTime = Some(5 seconds))
    val lifecycleProbe = TestProbe()
    val lifecycleProps = TestActors.forwardActorProps(lifecycleProbe.ref)
    val lifecycleFactory: ExecutionLifecycleFactory = (_: DriverState) => lifecycleProps

    val planId = PlanId(UUID.randomUUID())
    val executionPlan = TestActorRef(executionDriverProps(lifecycleFactory), self, "executionPlanThatNeverFires")
    watch(executionPlan)

    "create an execution from a job specification and terminate it right after" in {
      executionPlan ! Initialize(TestJobId, planId, TestJobSpec, trigger, None)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId should be (TestJobId)
      startedMsg.planId should be (planId)

      val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
      finishedMsg.jobId should be (TestJobId)
      finishedMsg.planId should be (planId)

      lifecycleProbe.expectNoMsg()
      expectMsg(ShardRegion.Passivate(PoisonPill))

      executionPlan ! PoisonPill
      expectTerminated(executionPlan)
    }
  }

}
