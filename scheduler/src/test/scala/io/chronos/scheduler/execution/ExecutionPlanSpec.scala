package io.chronos.scheduler.execution

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import akka.actor._
import akka.testkit._
import io.chronos.id.ModuleId
import io.chronos.scheduler.Registry
import io.chronos.{JobSpec, Trigger}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Inside, Matchers}

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionPlanSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec(UUID.randomUUID(), "foo", "foo desc", TestModuleId, "com.example.Job")

  class WatchAndForwardActor(executionPlan: ActorRef, listener: ActorRef)
    extends Actor {

    context.watch(executionPlan)

    override def receive = {
      case message => listener ! message
    }

  }

}

class ExecutionPlanSpec extends TestKit(ActorSystem("ExecutionPlanSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with Inside with MockFactory {

  import ExecutionPlanSpec._
  import Trigger._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution plan" should "kill itself if job is not enabled" in {
    val trigger = mock[Trigger]
    val executionProps: ExecutionPlan.ExecutionProps =
      (planId, jobSpec) => TestActors.echoActorProps

    val receiver = TestProbe()
    val executionPlan = TestActorRef(ExecutionPlan.props(trigger)(executionProps))
    TestActorRef(Props(classOf[WatchAndForwardActor], executionPlan, receiver.ref))

    executionPlan ! Registry.JobNotEnabled(UUID.randomUUID())

    //receiver.expectTerminated(executionPlan)
  }

  it should "create an execution from a job specification" in {
    val triggerMock = mock[Trigger]
    val executionProbe = TestProbe()
    val executionProps: ExecutionPlan.ExecutionProps =
      (planId, jobSpec) => TestActors.forwardActorProps(executionProbe.ref)

    val expectedScheduleTime = ZonedDateTime.now(clock)
    val expectedExecutionTime = expectedScheduleTime
    (triggerMock.nextExecutionTime(_: ReferenceTime)(_: Clock)).expects(ScheduledTime(expectedScheduleTime), clock).returning(Some(expectedExecutionTime))

    val executionPlan = TestActorRef(ExecutionPlan.props(triggerMock)(executionProps))

    executionPlan ! TestJobSpec

    executionProbe.expectMsg(Execution.WakeUp)
  }

}
