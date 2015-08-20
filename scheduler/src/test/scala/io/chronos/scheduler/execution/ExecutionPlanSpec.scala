package io.chronos.scheduler.execution

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor._
import akka.testkit.TestActors.ForwardActor
import akka.testkit._
import io.chronos.id.ModuleId
import io.chronos.scheduler.Registry
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
  import Trigger._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution plan" should {

    "terminate itself if job is not enabled" in {
      val trigger = mock[Trigger]
      val executionProps: ExecutionProps =
        (planId, jobSpec) => TestActors.echoActorProps

      val executionPlan = TestActorRef(ExecutionPlan.props(trigger)(executionProps))
      TestActorRef(Props(classOf[WatchAndForwardActor], executionPlan, self))

      executionPlan ! Registry.JobNotEnabled(UUID.randomUUID())

      //expectTerminated(executionPlan)
    }

  }

  "An execution plan" should  {
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

    "re-schedule the execution if the trigger is recurring" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)
      val expectedExecutionTime = expectedLastExecutionTime

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(LastExecutionTime(expectedLastExecutionTime), clock).returning(Some(expectedExecutionTime))

      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      executionProbe.expectMsg(Execution.WakeUp)
    }

    "stop the execution plan if trigger returns None" in {
      val expectedLastExecutionTime = ZonedDateTime.now(clock)

      (triggerMock.isRecurring _).expects().returning(true)
      (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(LastExecutionTime(expectedLastExecutionTime), clock).returning(None)

      executionProbe.send(executionPlan, Execution.Result(Execution.Success("bar")))

      //expectTerminated(executionPlan)
    }
  }

}
