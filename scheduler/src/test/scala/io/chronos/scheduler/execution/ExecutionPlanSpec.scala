package io.chronos.scheduler.execution

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor._
import akka.testkit.TestActors.ForwardActor
import akka.testkit._
import io.chronos.id.ModuleId
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.{JobSpec, Trigger}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionPlanSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec(UUID.randomUUID(), "foo", "foo desc", TestModuleId, "com.example.Job")

  class WatchAndForwardActor(executionPlan: ActorRef, listener: ActorRef)
    extends ForwardActor(listener) { context.watch(executionPlan) }

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
        (planId, jobSpec) => TestActors.echoActorProps

      val executionPlan = TestActorRef(ExecutionPlan.props(trigger)(executionProps))
      watch(executionPlan)

      executionPlan ! RegistryProtocol.JobNotEnabled(UUID.randomUUID())

      expectTerminated(executionPlan)
    }

  }

  "An execution plan with recurring trigger" should  {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (planId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val executionPlan = TestActorRef(ExecutionPlan.props(triggerMock)(executionProps), "executionPlan")
    watch(executionPlan)

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(ScheduledTime(expectedScheduleTime), clock).returning(Some(expectedExecutionTime))

      executionPlan ! TestJobSpec

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobSpec.id)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(Execution.WakeUp)
    }

    "re-schedule the execution once it finishes" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedLastExecutionTime

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(LastExecutionTime(expectedLastExecutionTime), clock).returning(Some(expectedExecutionTime))

      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      val scheduledMsg = expectMsgType[JobScheduled]
      scheduledMsg.jobId should be (TestJobSpec.id)
      scheduledMsg.planId should be (executionPlan.underlying.actor.asInstanceOf[ExecutionPlan].planId)

      executionProbe.expectMsg(Execution.WakeUp)
    }

    "stop the execution plan if trigger returns None" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(LastExecutionTime(expectedLastExecutionTime), clock).returning(None)

      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      expectTerminated(executionPlan)
    }
  }

  "An execution plan with non recurring trigger" should {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (planId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val executionPlan = TestActorRef(ExecutionPlan.props(triggerMock)(executionProps))
    TestActorRef(Props(classOf[WatchAndForwardActor], executionPlan, self))

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(ScheduledTime(expectedScheduleTime), clock).returning(Some(expectedExecutionTime))

      executionPlan ! TestJobSpec

      executionProbe.expectMsg(Execution.WakeUp)
    }

    "terminate once the execution finishes" in {
      (triggerMock.isRecurring _).expects().returning(false)

      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      executionProbe.expectNoMsg()
    }
  }

  "An execution plan that gets disabled" should {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionProps =
      (planId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val executionPlan = TestActorRef(ExecutionPlan.props(triggerMock)(executionProps))
    TestActorRef(Props(classOf[WatchAndForwardActor], executionPlan, self))

    "create an execution from a job specification" in {
      val expectedScheduleTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedScheduleTime

      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(ScheduledTime(expectedScheduleTime), clock).returning(Some(expectedExecutionTime))

      executionPlan ! TestJobSpec

      executionProbe.expectMsg(Execution.WakeUp)
    }

    "not re-schedule the execution after the job is disabled" in {
      system.eventStream.publish(RegistryProtocol.JobDisabled(TestJobSpec.id))
      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      executionProbe.expectNoMsg()
    }

  }

}
