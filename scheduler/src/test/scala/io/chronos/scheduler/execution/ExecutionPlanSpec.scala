package io.chronos.scheduler.execution

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor._
import akka.testkit._
import io.chronos.id.{JobId, ModuleId}
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.{JobSpec, Trigger}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionPlanSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", "foo desc", TestModuleId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

}

class ExecutionPlanSpec extends TestKit(ActorSystem("ExecutionPlanSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll with Matchers with Inside with MockFactory {

  import ExecutionPlanSpec._
  import SchedulerProtocol._
  import Trigger._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution plan for a disabled job" should {

    "terminate itself immediately" in {
      val trigger = mock[Trigger]
      val executionProps: ExecutionProps =
        (taskId, jobSpec) => TestActors.echoActorProps

      val planId = UUID.randomUUID()
      val executionPlan = TestActorRef(ExecutionPlan.props(planId, trigger)(executionProps), "executionPlanForDisabledJob")
      watch(executionPlan)

      executionPlan ! RegistryProtocol.JobNotEnabled(TestJobId)

      expectTerminated(executionPlan)
    }

  }

  "An execution plan with recurring trigger" should  {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (taskId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(ExecutionPlan.props(planId, triggerMock)(executionProps), "executionPlanWithRecurringTrigger")
    watch(executionPlan)

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).
        expects(ScheduledTime(expectedScheduleTime), clock).
        returning(Some(expectedExecutionTime))

      executionPlan ! (TestJobId -> TestJobSpec)

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(ExecutionFSM.WakeUp)
    }

    "re-schedule the execution once it finishes" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedLastExecutionTime

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).
        expects(LastExecutionTime(expectedLastExecutionTime), clock).
        returning(Some(expectedExecutionTime))

      executionProbe.send(executionPlan, ExecutionFSM.Result(Execution.Success("bar")))

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(ExecutionFSM.WakeUp)
    }

    "stop the execution plan if trigger returns None" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).
        expects(LastExecutionTime(expectedLastExecutionTime), clock).
        returning(None)

      executionProbe.send(executionPlan, ExecutionFSM.Result(Execution.Success("bar")))

      executionProbe.expectNoMsg(1 second)
      expectTerminated(executionPlan)
    }
  }

  "An execution plan with non recurring trigger" should {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (taskId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(ExecutionPlan.props(planId, triggerMock)(executionProps), "executionPlanWithOneShotTrigger")
    watch(executionPlan)

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).
        expects(ScheduledTime(expectedScheduleTime), clock).
        returning(Some(expectedExecutionTime))

      executionPlan ! (TestJobId -> TestJobSpec)

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(ExecutionFSM.WakeUp)
    }

    "terminate once the execution finishes" in {
      (triggerMock.isRecurring _).expects().returning(false)

      executionProbe.send(executionPlan, ExecutionFSM.Result(Execution.Success("bar")))

      executionProbe.expectNoMsg(1 second)
      expectTerminated(executionPlan)
    }
  }

  "An execution plan that gets disabled" should {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (taskId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val planId = UUID.randomUUID()
    val executionPlan = TestActorRef(ExecutionPlan.props(planId, triggerMock)(executionProps), "executionPlanForJobThatGetsDisabled")
    watch(executionPlan)

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).
        expects(ScheduledTime(expectedScheduleTime), clock).
        returning(Some(expectedExecutionTime))

      executionPlan ! (TestJobId -> TestJobSpec)

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobId)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(ExecutionFSM.WakeUp)
    }

    "not re-schedule the execution after the job is disabled" in {
      system.eventStream.publish(RegistryProtocol.JobDisabled(TestJobId))

      // This message will be sent to the deadletter actor.
      executionProbe.send(executionPlan, ExecutionFSM.Result(Execution.Success("bar")))

      executionProbe.expectNoMsg(1 second)
      expectTerminated(executionPlan)
    }

  }

}
