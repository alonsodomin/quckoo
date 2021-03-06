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

import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.testkit._

import io.quckoo._
import io.quckoo.api.TopicTag
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.testkit.{ImplicitClock, QuckooActorClusterSuite}

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 18/08/15.
  */
object SchedulerSpec {

  final val TestArtifactId = ArtifactId("com.example", "bar", "test")
  final val TestJobSpec =
    JobSpec("foo", Some("foo desc"), JobPackage.jar(TestArtifactId, "com.example.Job"))
  final val TestJobId = JobId(TestJobSpec)

  final val TestTrigger = Trigger.After(10 seconds)

}

class SchedulerSpec
    extends QuckooActorClusterSuite("SchedulerSpec") with ImplicitSender with ImplicitClock
    with ScalaFutures with BeforeAndAfter {

  import SchedulerSpec._
  import DistributedPubSubMediator._

  implicit val materializer = ActorMaterializer()

  val registryProbe  = TestProbe("registryProbe")
  val taskQueueProbe = TestProbe("taskQueueProbe")

  val eventListener = TestProbe()
  val mediator      = DistributedPubSub(system).mediator
  ignoreMsg {
    case DistributedPubSubMediator.SubscribeAck(_)   => true
    case DistributedPubSubMediator.UnsubscribeAck(_) => true
  }

  before {
    mediator ! DistributedPubSubMediator.Subscribe(TopicTag.Scheduler.name, eventListener.ref)
    system.eventStream.subscribe(eventListener.ref, classOf[Scheduler.Signal])
  }

  after {
    mediator ! DistributedPubSubMediator.Unsubscribe(TopicTag.Scheduler.name, eventListener.ref)
  }

  val readJournal = new QuckooTestJournal

  "A scheduler" should {

    val scheduler =
      TestActorRef(
        new Scheduler(
          readJournal,
          registryProbe.ref,
          TestActors.forwardActorProps(taskQueueProbe.ref)
        ),
        "scheduler"
      )

    var testPlanId: Option[PlanId] = None

    "create an execution driver for an enabled job" in {
      eventListener.expectMsg(Scheduler.Ready)

      scheduler ! ScheduleJob(TestJobId, trigger = TestTrigger)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(TestJobSpec)

      val startedMsg = eventListener.expectMsgType[ExecutionPlanStarted]
      startedMsg.jobId shouldBe TestJobId

      expectMsg(startedMsg)

      val scheduledMsg = eventListener.expectMsgType[TaskScheduled]
      scheduledMsg.jobId shouldBe TestJobId
      scheduledMsg.planId shouldBe startedMsg.planId

      testPlanId = Some(startedMsg.planId)
    }

    "return the execution plan details when requested" in {
      testPlanId shouldBe defined

      testPlanId.foreach { planId =>
        scheduler ! GetExecutionPlan(planId)

        val executionPlan = expectMsgType[ExecutionPlan]

        executionPlan.jobId shouldBe TestJobId
        executionPlan.planId shouldBe planId
        executionPlan.finished shouldBe false
      }
    }

    "return a map containing the current live execution plan" in {
      testPlanId shouldBe defined

      val executionPlans = Source
        .actorRef[(PlanId, ExecutionPlan)](5, OverflowStrategy.fail)
        .mapMaterializedValue(upstream => scheduler.tell(GetExecutionPlans, upstream))
        .runFold(Map.empty[PlanId, ExecutionPlan])((map, pair) => map + pair)

      whenReady(executionPlans) { plans =>
        plans should not be empty
        plans should contain key testPlanId.get
      }
    }

    "allow cancelling an execution plan" in {
      testPlanId shouldBe defined
      testPlanId foreach { planId =>
        scheduler ! CancelExecutionPlan(planId)

        val completedMsg = eventListener.expectMsgType[TaskCompleted]
        completedMsg.jobId shouldBe TestJobId
        completedMsg.planId shouldBe planId
        completedMsg.outcome shouldBe TaskExecution.Outcome.NeverRun(
          TaskExecution.Reason.UserRequest
        )

        val finishedMsg = eventListener.expectMsgType[ExecutionPlanFinished]
        finishedMsg.planId shouldBe planId
        finishedMsg.jobId shouldBe TestJobId

        expectMsg(ExecutionPlanCancelled(TestJobId, planId, finishedMsg.dateTime))
      }
    }

    "return an map of finished execution plans when there is none active" in {
      testPlanId shouldBe defined

      val executionPlans = Source
        .actorRef[(PlanId, ExecutionPlan)](5, OverflowStrategy.fail)
        .mapMaterializedValue(upstream => scheduler.tell(GetExecutionPlans, upstream))
        .runFold(Map.empty[PlanId, ExecutionPlan])((map, pair) => map + pair)

      whenReady(executionPlans) { plans =>
        val planId = testPlanId.get

        val expectedOutcome =
          Some(TaskExecution.Outcome.NeverRun(TaskExecution.Reason.UserRequest))

        plans should contain key planId
        plans(planId) should matchPattern {
          case ExecutionPlan(`TestJobId`, `planId`, _, _, _, `expectedOutcome`, _, _, _, _) =>
        }
      }
    }

    "return execution plan not found when asked for a non-existent plan" in {
      val randomPlanId = PlanId(UUID.randomUUID())

      scheduler ! GetExecutionPlan(randomPlanId)

      expectMsg(ExecutionPlanNotFound(randomPlanId))
    }

    "do nothing but reply if the job is not enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(TestJobSpec.copy(disabled = true))

      expectMsgType[JobNotEnabled].jobId shouldBe TestJobId
      eventListener.expectNoMsg()
    }

    "should reply job not found if the job is not present" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[GetJob].jobId shouldBe TestJobId
      registryProbe.reply(JobNotFound(TestJobId))

      expectMsgType[JobNotFound].jobId shouldBe TestJobId
      eventListener.expectNoMsg()
    }
  }

}
